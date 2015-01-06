package com.about80minutes.palantir.job;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;

import org.apache.hadoop.thirdparty.guava.common.collect.Lists;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.about80minutes.palantir.job.json.WikipediaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.api.ptobject.Document;
import com.palantir.api.workspace.PalantirClientContext;
import com.palantir.api.workspace.PalantirConnection;
import com.palantir.api.workspace.PalantirHeadlessClientContext;
import com.palantir.dataservices.DataSourceMakerException;
import com.palantir.exception.PropertyMakerException;
import com.palantir.services.Locator;
import com.palantir.services.loadlevel.LoadLevels;
import com.palantir.services.ptobject.DataSourceRecord;
import com.palantir.services.ptobject.Ontology;
import com.palantir.services.ptobject.PTObjectContainer;
import com.palantir.services.ptobject.Property;
import com.palantir.services.ptobject.PropertyBuilders;
import com.palantir.services.ptobject.linkadder.AddLinkArgs;
import com.palantir.services.ptobject.linkadder.AddLinkResult;
import com.palantir.services.ptobject.linkadder.PTObjectLinkAdder;

/**
 * Performs the download of a wikipedia article and links to the selected object
 */
public class WikipediaDownloadTask implements Callable<WikipediaDownloadResult> {
	private static final String CONNECTION_URL = "http://en.wikipedia.org/w/api.php?action=parse&prop=text&page=%s&format=json";
	private static final Logger log = LoggerFactory.getLogger(WikipediaDownloadTask.class);
	private Locator locator = null;
	private PalantirClientContext ctx = null;

	/**
	 * Constructor for this class
	 * 
	 * @param ctx a {@link com.palantir.api.workspace.PalantirHeadlessClientContext}
	 * to use by this download task
	 * @param locator a {@link com.palantir.services.Locator} of an object in
	 * the system
	 */
	public WikipediaDownloadTask(PalantirHeadlessClientContext ctx, Locator locator) {
		this.locator = locator;
		this.ctx = ctx;
	}

	/**
	 * Implementation of call, this co-ordinates the tasks actions.
	 */
	public WikipediaDownloadResult call() throws IOException {
		//convert locator to ptoc
		List<Locator> objectsToLoad = Lists.newArrayList(this.locator);
		PTObjectContainer parent = this.ctx.getPalantirConnection().loadObjects(objectsToLoad, LoadLevels.getLinkDsrTitleLoadedInstance()).iterator().next();
		
		//get title for use in search
		String title = parent.getTitle();
		String searchTerm = encodeSearchTerm(title);
		
		//search wikipedia
		String articleText = getWikipediaArticle(searchTerm);

		//create a document
		WikipediaDownloadResult returnPTOCs = null;
		try {
			returnPTOCs = createDocument(parent, searchTerm, title, articleText);
		} catch (DataSourceMakerException e) {
			log.error("Error creating datasource", e);
		}
		return returnPTOCs;
	}

	/**
	 * Utility method for encoding a search term
	 * 
	 * @param title the {@link java.lang.String} to encode
	 * 
	 * @return an encoded {@link java.lang.String}
	 */
	private String encodeSearchTerm(String title) {
		String returnable = "";
		try {
			returnable = URLEncoder.encode(title, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("Error encoding search term", e);
		}
		return returnable;
	}

	/**
	 * Creates a document in Palantir and returns a result object.
	 * 
	 * @param parent a {@link com.palantir.services.ptobject.PTObjectContainer}
	 * which is the originating object for this document
	 * @param searchTerm a {@link java.lang.String} which contains the term to
	 * search for.
	 * @param title a {@link java.lang.String} containing the unencoded term to
	 * search for.
	 * @param articleText a {@link java.lang.String} containing the text of the
	 * article.
	 * 
	 * @return a {@link com.about80minutes.palantir.job.WikipediaDownloadResult}
	 * 
	 * @throws DataSourceMakerException if there is a problem making a datasource
	 */
	private WikipediaDownloadResult createDocument(PTObjectContainer parent, String searchTerm, String title, String articleText) throws DataSourceMakerException {
		PalantirConnection conn = ctx.getPalantirConnection();
		
		// create a document object
		Document doc = conn.createDocumentBuilder(title, String.format("An article about %s", title), articleText).build();
		PTObjectContainer ptoc = doc.getObject();
		
		Ontology ontology = ctx.getOntology();
		
		// add a property
		try {
			Property prop = PropertyBuilders.builder(ontology.getPropertyTypeByUri("com.palantir.property.URL"), conn)
							.value(String.format(CONNECTION_URL, searchTerm))
							.addDataSourceRecords(Collections.singleton(conn.getDsrFactory().createManuallyEnteredDsr()))
							.buildParsed();
			ptoc.addProperty(prop);
		} catch (PropertyMakerException e) {
			log.warn("Cant addd URL property", e);
		}
		
		DataSourceRecord linkDsr = conn.getDsrFactory().createManuallyEnteredDsr();
		
		// add a link between new doc and parent
		AddLinkResult linkResult = PTObjectLinkAdder.addLink(AddLinkArgs.create(parent, ptoc, ctx.getPalantirConnection(), Collections.singleton(linkDsr)));
		WikipediaDownloadResult result = new WikipediaDownloadResult();
		result.parent = parent;
		result.links.add(linkResult.getLink());
		result.children.add(ptoc);
		
		return result;
	}

	/**
	 * Obtains the text of a wikipedia article
	 * 
	 * @param searchTerm an encoded {@link java.lang.String} containing the text
	 * to search for
	 * 
	 * @return a {@link java.lang.String} containing the article text
	 * 
	 * @throws IOException if there is an error streaming the result.
	 */
	private String getWikipediaArticle(String searchTerm) throws IOException {
		StringBuilder builder = new StringBuilder();
		ObjectMapper mapper = new ObjectMapper();
		
		HttpClient httpclient = HttpClientBuilder.create().build();
		HttpGet httpget = new HttpGet(String.format(CONNECTION_URL, searchTerm));
		httpget.addHeader("accept", "application/json");
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String responseBody = httpclient.execute(httpget, responseHandler);
		
		WikipediaResponse wikipediaResult = mapper.readValue(responseBody, WikipediaResponse.class);
		
		Source htmlSource = new Source(wikipediaResult.responseItem.text.articleText);
	    Segment htmlSeg = new Segment(htmlSource, 0, htmlSource.length());
	    Renderer htmlRend = new Renderer(htmlSeg);
	    builder.append(htmlRend.toString());
		
		return builder.toString();
	}
}

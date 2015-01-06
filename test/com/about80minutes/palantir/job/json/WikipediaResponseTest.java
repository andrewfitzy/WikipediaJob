package com.about80minutes.palantir.job.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.wsdl.util.IOUtils;

/**
 * Tests the json entity files
 */
public class WikipediaResponseTest {
	
	private static String FILE_CONTENTS;
	
	/**
	 * Load json into a String
	 */
	@BeforeClass
	public static void loadFile() throws URISyntaxException, FileNotFoundException, IOException {
		//load the file
		URL fileURL = WikipediaResponseTest.class.getResource("/file.json");
		File jsonFile = new File(fileURL.toURI());
		FILE_CONTENTS = IOUtils.getStringFromReader(new FileReader(jsonFile));
	}
	
	/**
	 * Test parsing of JSON into objects
	 */
	@Test
	public void testParseJson() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		WikipediaResponse wikipediaResult = mapper.readValue(FILE_CONTENTS, WikipediaResponse.class);
		
		assertNotNull(wikipediaResult);
		assertEquals("Kevin Bacon", wikipediaResult.responseItem.title);
	}
}

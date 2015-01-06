package com.about80minutes.palantir.job;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.palantir.api.dataevent.PalantirDataEventType;
import com.palantir.api.job.Job;
import com.palantir.api.job.JobArgs;
import com.palantir.api.job.JobResults;
import com.palantir.api.job.JobResults.ReturnStatus;
import com.palantir.api.workspace.PalantirHeadlessClientContext;
import com.palantir.batch.BatchException;
import com.palantir.services.Locator;
import com.palantir.services.ptobject.Link;
import com.palantir.services.ptobject.PTObjectContainer;
import com.palantir.util.Locatables;
import com.palantir.workspace.model.EdgeComponent;
import com.palantir.workspace.model.EdgeComponents;

/**
 * Implementation of job to be run on the job server. 
 */
public class WikipediaJob extends Job {

	private static final Logger log = LoggerFactory.getLogger(WikipediaJob.class);
	private Collection<Locator> locators = null;
	
	/**
	 * Applies the arguments to this job.
	 * 
	 * @param args a {@link com.palantir.api.job.JobArgs} to use with this job
	 */
	@Override
	public void applyArgs(JobArgs args) throws BatchException {
		if (args instanceof WikipediaJobArgs) {
			WikipediaJobArgs userArgs = (WikipediaJobArgs) args;
			locators = userArgs.getSelectedLocators();
		}
	}
	
	/**
	 * Performs the actions required by this job
	 * 
	 * @param ctx a {@link com.palantir.api.workspace.PalantirHeadlessClientContext}
	 * to use in this job
	 * 
	 * @return a {@link com.palantir.api.job.JobResults} encapsulating the
	 * outcome of this job
	 */
	@Override
	public JobResults doJob(PalantirHeadlessClientContext ctx) {
		Map<String, Object> resultsMap = Maps.newHashMap();
		List<PTObjectContainer> ptocList = Lists.newArrayList();
		
		ExecutorService executor = Executors.newFixedThreadPool(5);
		CompletionService<WikipediaDownloadResult> ecs = new ExecutorCompletionService<WikipediaDownloadResult>(executor);
		
		this.setPercentComplete(5); //start with a bit of progress complete
		
		int steps = 1 + locators.size();
		float progressStep = 85f / (float)steps; //first 5% as a start, reserve last 10 % for store
		
		Collection<EdgeComponent> ecList = Lists.newArrayList();
		
		try {
			for(Locator loc : locators) {
				WikipediaDownloadTask task = new WikipediaDownloadTask(ctx, loc);
				ecs.submit(task);
			}
			for (int i = 0; i < locators.size(); ++i) {
				try {
					WikipediaDownloadResult taskResult = ecs.take().get();
					if (taskResult != null) {
						ptocList.addAll(taskResult.children);
						ptocList.add(taskResult.parent);
						
						ecList.addAll(transformResult(taskResult));
						
						this.setPercentComplete(getPercentComplete() + progressStep);
					}
				} catch(InterruptedException e) {
					log.error("Exception", e);
				} catch(ExecutionException e) {
					log.error("Exception", e);
				}
			}
			this.setStatusMessage("Store Objects");
			this.setPercentComplete(90f);
			ctx.getPalantirConnection().storeObjects(ptocList,PalantirDataEventType.DATA_IMPORT);
			
			List<Locator> locs = Locatables.getLocatorList(ptocList);
			
			resultsMap.put(WikipediaJobArgs.JOB_RESULT_EC, ecList);
			resultsMap.put(WikipediaJobArgs.JOB_RESULT_OBJ, locs);
			
			results.setResultsMap(resultsMap);
			this.setPercentComplete(100f);
		} finally {
			executor.shutdown();
		}
		results.setReturnStatus(ReturnStatus.SUCCESS);
		return results;
	}
	
	/**
	 * Utility method for converting a result object into a collection of edge
	 * components.
	 * 
	 * @param taskResult a {@link com.about80minutes.palantir.job.WikipediaDownloadResult}
	 * containing the result of the job run
	 * 
	 * @return a {@link java.util.Collection} of {@link com.palantir.workspace.model.EdgeComponent}
	 * objects for return in the job result.
	 */
	private Collection<EdgeComponent> transformResult(WikipediaDownloadResult taskResult) {
		List<EdgeComponent> ecList = Lists.newArrayList();
		Locator parentLoc = taskResult.parent.getLocator();
		for(Link tmpLink : taskResult.links) {
			ecList.add(EdgeComponents.createLinkErc(tmpLink, parentLoc));
		}
		return ecList;
	}

	/**
	 * Gets a logger for this job
	 * 
	 * @return a {@link org.slf4j.Logger} used by this job.
	 */
	@Override
	public Logger getLogger() {
		return log;
	}
}

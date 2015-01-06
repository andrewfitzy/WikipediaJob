package com.about80minutes.palantir.job;

import java.util.Collection;

import com.palantir.api.job.JobArgs;
import com.palantir.services.Locator;

/**
 * Arguments class for the Wikipedia lookup job.
 */
public class WikipediaJobArgs extends JobArgs {

	private static final long serialVersionUID = 1L;
	
	private final Collection<Locator> selectedLocators;

	public static final String JOB_RESULT_EC = "JOB_RESULT_EC";
	
	public static final String JOB_RESULT_OBJ = "JOB_RESULT_OBJ";
	
	/**
	 * Constructor to instantiate this class
	 * 
	 * @param selectedLocators a {@link java.util.Collection} of selected
	 * {@link com.palantir.services.Locator} for objects selected on the graph.
	 */
	public WikipediaJobArgs(Collection<Locator> selectedLocators) {
		super(WikipediaJob.class);
		this.selectedLocators = selectedLocators;
	}
	
	/**
	 * Indicates whether a Lock is required. As this job will be writing objects
	 * a lock will be required on the realm.
	 * 
	 * @return a boolean indicating whether a lock is required.
	 */
	@Override
	public boolean requiresLockGrant() {
		return true;
	}

	/**
	 * Accessor for the selected locators
	 * 
	 * @return a {@link java.util.Collection} of selected
	 * {@link com.palantir.services.Locator} for objects selected on the graph.
	 */
	public Collection<Locator> getSelectedLocators() {
		return selectedLocators;
	}
}

package com.about80minutes.palantir.job;

import java.awt.Dimension;

import javax.swing.SwingConstants;

import com.palantir.api.workspace.ApplicationInterface;
import com.palantir.api.workspace.HelperInterface;
import com.palantir.api.workspace.PalantirWorkspaceContext;
import com.palantir.api.workspace.SafeAbstractHelperFactory;
import com.palantir.api.workspace.applications.GraphApplicationInterface;

/**
 * This UserJobsTestingHelperFactory is a factory which generates the UserJobsTestingHelper.
 */
public class WikipediaJobHelperFactory extends SafeAbstractHelperFactory {

	/**
	 * Constructor. Used to initialise this class.
	 */
	public WikipediaJobHelperFactory() {
		super("Wikipedia Lookup",
			new String[] { GraphApplicationInterface.APPLICATION_URI },
			new Integer [] { SwingConstants.HORIZONTAL },
			new Dimension(330,500),
			null,
			WikipediaJobHelper.class.getCanonicalName());
	}

	/**
	 * Initialises the helper built by this factory
	 * 
	 * @param palantirContext a {@link com.palantir.api.workspace.PalantirWorkspaceContext}
	 * used by the helper
	 * @param application an {@link com.palantir.api.workspace.ApplicationInterface}
	 * used by this helper 
	 * 
	 * @return a {@link com.palantir.api.workspace.HelperInterface}
	 */
	public HelperInterface createHelper(PalantirWorkspaceContext palantirContext, ApplicationInterface application) {
		return new WikipediaJobHelper(this, palantirContext);
	}
}

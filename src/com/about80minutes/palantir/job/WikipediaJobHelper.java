package com.about80minutes.palantir.job;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.palantir.api.job.JobArgs;
import com.palantir.api.job.JobResults;
import com.palantir.api.job.JobResults.ReturnStatus;
import com.palantir.api.job.JobSpec;
import com.palantir.api.job.JobStatus;
import com.palantir.api.job.JobStatusMonitor;
import com.palantir.api.job.PollableJobFuture;
import com.palantir.api.ui.TableLayouts;
import com.palantir.api.ui.progress.MultiStageProgressBar;
import com.palantir.api.workspace.AbstractHelper;
import com.palantir.api.workspace.ApplicationContext;
import com.palantir.api.workspace.ApplicationInterface;
import com.palantir.api.workspace.HelperFactory;
import com.palantir.api.workspace.PalantirFrame;
import com.palantir.api.workspace.PalantirWorkspaceContext;
import com.palantir.api.workspace.graph.Graph;
import com.palantir.api.workspace.progress.Stage;
import com.palantir.api.workspace.selection.SelectionAgentEvent;
import com.palantir.api.workspace.selection.SelectionAgentListener;
import com.palantir.services.Locator;
import com.palantir.util.awt.NotAwt;
import com.palantir.workspace.model.EdgeComponent;

/**
 * Implementation of the helper interface class.
 */
public class WikipediaJobHelper extends AbstractHelper {

	private static final Logger log = LoggerFactory.getLogger(WikipediaJobHelper.class);

	private final HelperFactory factory;
	private final PalantirWorkspaceContext palantirContext;
	private JPanel panel = null;

	private Stage progress = null;
	private JLabel statusLabel = null;
	private Collection<Locator> selectedObjects = null;

	private GraphSelectionAgent selectionAgent = null;
	private QueryWikipedia queryAction = null;

	/**
	 * Constructor for this class
	 * 
	 * @param factory a {@link com.palantir.api.workspace.HelperFactory} which
	 * initiated this helper
	 * @param context a {@link com.palantir.api.workspace.PalantirWorkspaceContext}
	 * to be used by this helper
	 */
	public WikipediaJobHelper(HelperFactory factory, PalantirWorkspaceContext context) {
		this.factory = factory;
		this.palantirContext = context;
		buildGUI();
	}

	/**
	 * Utility method to build the user interface
	 */
	private void buildGUI() {
		this.selectionAgent = new GraphSelectionAgent();
		this.queryAction = new QueryWikipedia("Run");

		this.panel = new JPanel(TableLayouts.create("1,p,f,1","1,p,p,1", 8, 8));

		this.panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		this.panel.add(new JButton(this.queryAction),"1,1");
		
		final MultiStageProgressBar bar = new MultiStageProgressBar();
		progress = bar.addStage(0.1f);
		this.panel.add(bar.getProgressBar(),"2,1,2,1");
		
		statusLabel = new JLabel("Status");
		this.panel.add(statusLabel,"1,2,2,2");
	}

	/**
	 * Implementation of defined method, this handles the situation where the
	 * helper is removed from view.
	 * 
	 * @param ai a {@link com.palantir.api.workspace.ApplicationInterface} which
	 * can be used by this method. 
	 */
	public void dispose(ApplicationInterface application) {
		application.getSelectionAgent().getSelectionAgentSupport().removeSelectionAgentListener(this.selectionAgent);     
	}
	
	/**
	 * Register any listeners used by this helper
	 * 
	 * @param ai a {@link com.palantir.api.workspace.ApplicationInterface}
	 */
	public void initialize(ApplicationInterface application) {
		application.getSelectionAgent().getSelectionAgentSupport().addSelectionAgentListener(this.selectionAgent);
	}
	
	/**
	 * Implementation of declared method, this returns the default screen
	 * position for this helper
	 * 
	 * @return a {@link java.lang.String} containing the default location for
	 * the helper
	 */
	public String getDefaultPosition() {
		return BorderLayout.SOUTH;
	}
	
	/**
	 * Implementation of declared method, this returns the main UI component for
	 * this helper
	 * 
	 * @return a {@link javax.swing.JComponent} which is the main UI element of
	 * this helper
	 */
	public JComponent getDisplayComponent() {
		return this.panel;
	}
	
	/**
	 * Returns the factory that created this helper
	 * 
	 * @return a {@link com.palantir.api.workspace.HelperFactory} that created
	 * this helper
	 */
	public HelperFactory getFactory() {
		return this.factory;
	}
	
	/**
	 * Returns the title for this helper
	 * 
	 * @return a {@link java.lang.String} containing the title of this helper
	 */
	public String getTitle() {
		return "Wikipedia Lookup";
	}

	/**
	 * Implementation of declared method, this returns the icon for this helper
	 * 
	 * @return a {@link java.awt.Image} which represents the helper icon
	 */
	public Image getFrameIcon() {
		return null;
	}

	/**
	 * Implementation of declared method, this returns an icon for this helper
	 * 
	 * @return a {@link javax.swing.Icon} to be used by this helper
	 */
	public Icon getIcon() {
		return null;
	}

	/**
	 * Null implementation of defined method.
	 * 
	 * @param constraint a {@link java.lang.String} that contains the constraint
	 */
	public void setConstraint(String arg0) {
	}

	/**
	 * Null implementation of defined method.
	 * 
	 * @param window a {@link com.palantir.api.workspace.PalantirFrame} to which
	 * this helper is added
	 * @param tab a {@link com.palantir.api.workspace.ApplicationContext} that
	 * can be used by this helper
	 */
	public void setOwners(PalantirFrame arg0, ApplicationContext arg1) {
	}

	/**
	 * This class contains the actions that should be performed in reacting to
	 * object selection events.
	 */
	private class GraphSelectionAgent implements SelectionAgentListener {

		/**
		 * Null implementation, not reacting to filters being applied
		 * currently
		 * 
		 * @param event a {@link com.palantir.api.workspace.selection.SelectionAgentEvent}
		 * to react to
		 */
		public void handleFilterEvent(SelectionAgentEvent event) {
			// ignore
		}

		/**
		 * Null implementation, not reacting to updates being applied
		 * currently
		 * 
		 * @param event a {@link com.palantir.api.workspace.selection.SelectionAgentEvent}
		 * to react to
		 */
		public void handleUpdateEvent(SelectionAgentEvent event) {
			// ignore
		}

		/**
		 * Reacts to selections within the Palantir application
		 * 
		 * @param event a {@link com.palantir.api.workspace.selection.SelectionAgentEvent}
		 * to react to
		 */
		public void handleSelectionEvent(SelectionAgentEvent event) {
			selectedObjects = Lists.newArrayList(event.getItemGroup().getObjectLocatorsDefaultFilter());
		}
	}

	/**
	 * Performs actions when the associated control is used. This class
	 * initiates a job to query wikipedia for all selected items. 
	 */
	@SuppressWarnings("serial")
	private class QueryWikipedia extends AbstractAction {
		
		/**
		 * Constructor for this action
		 * 
		 * @param title a {@link java.lang.String} to use as the action title
		 */
		public QueryWikipedia(String label) {
			super(label);
		}

		/**
		 * Completes the actions required by this action
		 * 
		 * @param an {@link java.awt.event.ActionEvent} to react to 
		 */
		public void actionPerformed(ActionEvent arg0) {
			if(selectedObjects != null && selectedObjects.size() > 0) {
				NotAwt.Utilities.runAsynchronous(new Runnable() {
					public void run() {
						try {
							runAndWaitForJob();
						}catch (Exception e) {
							log.error("Error running job",e);
							WikipediaJobHelper.this.statusLabel.setText(e.getMessage() + " -- see log for details");
						}
					}
				});
			} else {
				WikipediaJobHelper.this.statusLabel.setText("No objects selected");
			}
		}

		/**
		 * Method for initialising a job and submitting to the job server.
		 * 
		 * @throws Exception
		 */
		@SuppressWarnings({ "unchecked", "deprecation" })
		private void runAndWaitForJob() throws Exception {
			WikipediaJobHelper.this.statusLabel.setText("Preparing job");

			JobArgs args = new WikipediaJobArgs(selectedObjects);
			JobSpec spec = new JobSpec(palantirContext.getPalantirConnection(), String.format("Wiki lookup (%d items)", selectedObjects.size()), args, palantirContext.getUser(), palantirContext.getInvestigationManager().getInvestigation().getRealm(), false);

			//in theory should update the progress bar, in practice???
			JobStatusMonitor updateStatus = new JobStatusMonitor() {
				public void statusUpdate(JobStatus status) {
					progress.setFractionalValue(status.getPercentComplete());
				}
			};

			PollableJobFuture jobFuture = palantirContext.runJobAndLockWindows(spec, updateStatus);
			WikipediaJobHelper.this.statusLabel.setText("Job Submitted");
			JobResults results = jobFuture.get();
			if(ReturnStatus.SUCCESS.equals(results.getReturnStatus())) {
				Graph graph = palantirContext.getGraph();
				Collection<EdgeComponent> ecs = (Collection<EdgeComponent>)results.getResultsMap().get(WikipediaJobArgs.JOB_RESULT_EC);
				Collection<Locator> obj = (Collection<Locator>)results.getResultsMap().get(WikipediaJobArgs.JOB_RESULT_OBJ);
				
				graph.addObjectsAndEdgeComponents(obj, ecs);
				WikipediaJobHelper.this.statusLabel.setText("Job done!");
			} else {
				log.error("Error", results.getExceptions());
				WikipediaJobHelper.this.statusLabel.setText("Job failed.");
			}
		}
	}
}

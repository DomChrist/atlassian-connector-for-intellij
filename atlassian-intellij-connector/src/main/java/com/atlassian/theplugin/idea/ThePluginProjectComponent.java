/**
 * Copyright (C) 2008 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlassian.theplugin.idea;

import com.atlassian.theplugin.cfg.CfgUtil;
import com.atlassian.theplugin.commons.UIActionScheduler;
import com.atlassian.theplugin.commons.bamboo.*;
import com.atlassian.theplugin.commons.cfg.CfgManager;
import com.atlassian.theplugin.commons.cfg.ConfigurationListenerAdapter;
import com.atlassian.theplugin.commons.cfg.ProjectConfiguration;
import com.atlassian.theplugin.commons.configuration.PluginConfiguration;
import com.atlassian.theplugin.commons.crucible.CrucibleServerFacade;
import com.atlassian.theplugin.commons.crucible.CrucibleServerFacadeImpl;
import com.atlassian.theplugin.commons.util.LoggerImpl;
import com.atlassian.theplugin.configuration.ProjectConfigurationBean;
import com.atlassian.theplugin.crucible.model.CrucibleReviewListModel;
import com.atlassian.theplugin.idea.autoupdate.ConfirmPluginUpdateHandler;
import com.atlassian.theplugin.idea.autoupdate.PluginUpdateIcon;
import com.atlassian.theplugin.idea.bamboo.BambooStatusIcon;
import com.atlassian.theplugin.idea.bamboo.BuildListModelImpl;
import com.atlassian.theplugin.idea.bamboo.BuildStatusChangedToolTip;
import com.atlassian.theplugin.idea.crucible.CruciblePatchSubmitExecutor;
import com.atlassian.theplugin.idea.crucible.CrucibleStatusChecker;
import com.atlassian.theplugin.idea.crucible.CrucibleStatusIcon;
import com.atlassian.theplugin.idea.crucible.editor.CrucibleEditorFactoryListener;
import com.atlassian.theplugin.idea.jira.IssuesToolWindowPanel;
import com.atlassian.theplugin.idea.ui.linkhiglighter.FileEditorListenerImpl;
import com.atlassian.theplugin.notification.crucible.CrucibleNotificationTooltip;
import com.atlassian.theplugin.notification.crucible.CrucibleReviewNotifier;
import com.atlassian.theplugin.remoteapi.MissingPasswordHandler;
import com.atlassian.theplugin.util.PluginUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;

/**
 * Per-project plugin component.
 */

public class ThePluginProjectComponent implements ProjectComponent {
	private static final String THE_PLUGIN_TOOL_WINDOW_ICON = "/icons/ico_plugin_16.png";

	private final ProjectConfigurationBean projectConfigurationBean;
	private final Project project;

	public CfgManager getCfgManager() {
		return cfgManager;
	}

	private final CfgManager cfgManager;

	private final UIActionScheduler actionScheduler;
	private BambooStatusIcon statusBarBambooIcon;
	private CrucibleStatusIcon statusBarCrucibleIcon;
	private CrucibleNotificationTooltip crucibleTooltip;

	private PluginUpdateIcon statusPluginUpdateIcon;
	private BambooStatusChecker bambooStatusChecker;
	private final BuildListModelImpl bambooModel;
	private CrucibleStatusChecker crucibleStatusChecker;
	private BambooStatusTooltipListener tooltipBambooStatusListener;

	private final CrucibleServerFacade crucibleServerFacade;

	private final ToolWindowManager toolWindowManager;

	private boolean created;
	private CrucibleReviewNotifier crucibleReviewNotifier;
	private final CrucibleReviewListModel crucibleReviewListModel;
	private final PluginConfiguration pluginConfiguration;

	private IssuesToolWindowPanel issuesToolWindowPanel;

	private PluginToolWindow toolWindow;

	//	public static final Key<ReviewActionEventBroker> BROKER_KEY = Key.create("thePlugin.broker");
	private ConfigurationListenerImpl configurationListener;

	private CrucibleEditorFactoryListener crucibleEditorFactoryListener;

	private FileEditorListenerImpl fileEditorListener;

	public ThePluginProjectComponent(Project project, ToolWindowManager toolWindowManager,
			PluginConfiguration pluginConfiguration, UIActionScheduler actionScheduler,
			ProjectConfigurationBean projectConfigurationBean, CfgManager cfgManager,
			@NotNull IssuesToolWindowPanel issuesToolWindowPanel,
			@NotNull PluginToolWindow pluginToolWindow,
			@NotNull BuildListModelImpl bambooModel,
			@NotNull final CrucibleStatusChecker crucibleStatusChecker,
			@NotNull final CrucibleReviewNotifier crucibleReviewNotifier,
			@NotNull final CrucibleReviewListModel crucibleReviewListModel) {
		this.project = project;
		this.cfgManager = cfgManager;
//        project.putUserData(BROKER_KEY, new ReviewActionEventBroker(project));

		this.actionScheduler = actionScheduler;
		this.toolWindowManager = toolWindowManager;
		this.pluginConfiguration = pluginConfiguration;
		this.projectConfigurationBean = projectConfigurationBean;
		this.bambooModel = bambooModel;
		this.crucibleStatusChecker = crucibleStatusChecker;
		this.crucibleReviewNotifier = crucibleReviewNotifier;
		this.crucibleReviewListModel = crucibleReviewListModel;
		this.crucibleServerFacade = CrucibleServerFacadeImpl.getInstance();
		this.issuesToolWindowPanel = issuesToolWindowPanel;
		this.toolWindow = pluginToolWindow;
		/*

										WARNING!!!
		BEFORE ADDING SOME INITIALIZATION CODE TO COSTRUCTOR THINK TWICE
                                         st
		...MAYBE YOU SHOULD PUT IT INTO THE initializePlugin METHOD
		(WHICH IS INVOKED WHEN THE ENTIRE PLUGIN ENVIRONMENT IS SET UP)?


		 */
		// make findBugs happy
		statusBarBambooIcon = null;
		statusBarCrucibleIcon = null;
		statusPluginUpdateIcon = null;
		created = false;
		StartupManager.getInstance(project).registerPostStartupActivity(new Runnable() {
			public void run() {
				LoggerImpl.getInstance().info("Start: Project initializing");
				initializePlugin();
				LoggerImpl.getInstance().info("End: Project initialized");
			}
		});
	}

	public void initComponent() {
		LoggerImpl.getInstance().info("Init ThePlugin project component.");
		this.fileEditorListener = new FileEditorListenerImpl(project);
	}

	public void disposeComponent() {
		LoggerImpl.getInstance().info("Dispose ThePlugin project component");
	}

	@NotNull
	public String getComponentName() {
		return "ThePluginProjectComponent";
	}

	private void initializePlugin() {
		// unregister changelistmanager?
		// only open tool windows for each application that's registered
		// show something nice if there are non
		// swap listener for dataretrievedlistener and datachangelisteners
		// store bamboo between runs in UDC
		// clean up object model confusion

		if (!created) {

			toolWindow.register(toolWindowManager);

			ChangeListManager.getInstance(project).registerCommitExecutor(
					new CruciblePatchSubmitExecutor(project, crucibleServerFacade, cfgManager));

			this.bambooStatusChecker = new BambooStatusChecker(CfgUtil.getProjectId(project), actionScheduler,
					cfgManager, pluginConfiguration,
					new MissingPasswordHandler(BambooServerFacadeImpl.getInstance(PluginUtil.getLogger()), cfgManager, project),
					PluginUtil.getLogger());

			// DependencyValidationManager.getHolder(project, "", )

			issuesToolWindowPanel.refreshModels();

			// create Atlassian tool window
//			toolWindow = new PluginToolWindow(toolWindowManager, project, cfgManager, bambooToolWindowPanel);
			Icon toolWindowIcon = IconLoader.getIcon(THE_PLUGIN_TOOL_WINDOW_ICON);
			toolWindow.getIdeaToolWindow().setIcon(toolWindowIcon);

			// create tool window content

//			toolWindow.registerPanel(PluginToolWindow.ToolWindowPanels.BAMBOO_OLD);

			toolWindow.registerPanel(PluginToolWindow.ToolWindowPanels.BUILDS);
			toolWindow.registerPanel(PluginToolWindow.ToolWindowPanels.CRUCIBLE);
			toolWindow.registerPanel(PluginToolWindow.ToolWindowPanels.ISSUES);

			IdeaHelper.getAppComponent().getSchedulableCheckers().add(bambooStatusChecker);
			// add tool window bamboo content listener to bamboo checker thread
			bambooStatusChecker.registerListener(new BambooStatusListener() {
				public void updateBuildStatuses(final Collection<BambooBuild> buildStatuses) {
					bambooModel.update(buildStatuses);
				}

				public void resetState() {
				}
			});

			// create Bamboo status bar icon
			statusBarBambooIcon = new BambooStatusIcon(this.project, cfgManager, toolWindow);
			statusBarBambooIcon.updateBambooStatus(BuildStatus.UNKNOWN, new BambooPopupInfo());

			// add icon listener to bamboo checker thread
			final StatusIconBambooListener iconBambooStatusListener = new StatusIconBambooListener(statusBarBambooIcon);
			bambooStatusChecker.registerListener(iconBambooStatusListener);

			// add simple bamboo listener to bamboo checker thread
			// this listener shows idea tooltip when buld failed
			final BambooStatusDisplay bambooStatusDisplay = new BuildStatusChangedToolTip(project, toolWindow);
			tooltipBambooStatusListener = new BambooStatusTooltipListener(bambooStatusDisplay, pluginConfiguration);
			bambooStatusChecker.registerListener(tooltipBambooStatusListener);
//			bambooStatusChecker.registerListener(buildToolWindowPanel.getBuildTree());

			// add bamboo icon to status bar
			statusBarBambooIcon.showOrHideIcon();

			// setup Crucible status checker and listeners
			IdeaHelper.getAppComponent().getSchedulableCheckers().add(crucibleStatusChecker);
			// create crucible status bar icon
			statusBarCrucibleIcon = new CrucibleStatusIcon(project, cfgManager, toolWindow);

			//registerCrucibleNotifier();

			// add crucible icon to status bar
			//statusBar.addCustomIndicationComponent(statusBarCrucibleIcon);
			statusBarCrucibleIcon.showOrHideIcon();

			statusPluginUpdateIcon = new PluginUpdateIcon(project, pluginConfiguration, cfgManager);
			ConfirmPluginUpdateHandler.getInstance().setDisplay(statusPluginUpdateIcon);
			//statusPluginUpdateIcon.showOrHideIcon();

			toolWindow.showHidePanels();
			// focus last active panel only if it exists (do not create panel)
			toolWindow.focusPanelIfExists(projectConfigurationBean.getActiveToolWindowTab());
			toolWindow.getIdeaToolWindow().getContentManager().addContentManagerListener(new ContentManagerAdapter() {
				@Override
				public void selectionChanged(final ContentManagerEvent event) {
					projectConfigurationBean.setActiveToolWindowTab(event.getContent().getDisplayName());
				}
			});

			IdeaHelper.getAppComponent().rescheduleStatusCheckers(false);

			configurationListener = new ConfigurationListenerImpl();
			cfgManager.addProjectConfigurationListener(CfgUtil.getProjectId(project), configurationListener);
			cfgManager.addProjectConfigurationListener(CfgUtil.getProjectId(project),
					issuesToolWindowPanel.getConfigListener());

			created = true;

			crucibleEditorFactoryListener = new CrucibleEditorFactoryListener(project,
					crucibleReviewListModel);
			EditorFactory.getInstance()
					.addEditorFactoryListener(crucibleEditorFactoryListener);

			registerCrucibleNotifier();
		}
	}

	private void registerCrucibleNotifier() {
		crucibleReviewListModel.addListener(crucibleReviewNotifier);
		crucibleTooltip = new CrucibleNotificationTooltip(statusBarCrucibleIcon, project, toolWindow, pluginConfiguration);

		crucibleReviewNotifier.registerListener(crucibleTooltip);
	}


	public void projectOpened() {
		// content moved to StartupManager to wait until
		// here we have guarantee that IDEA splash screen will not obstruct our window
		askForUserStatistics();
		fileEditorListener.startListening();
	}

	public FileEditorListenerImpl getFileEditorListener() {
		return fileEditorListener;
	}

	private void askForUserStatistics() {
		ApplicationManager.getApplication().invokeLater(new Runnable() {
			public void run() {
				if (pluginConfiguration.getGeneralConfigurationData().getAnonymousFeedbackEnabled() == null) {
					int answer = Messages.showYesNoDialog("We would greatly appreciate if you allow us to collect anonymous "
							+ "usage statistics to help us provide a better quality product. Is this ok?",
							PluginUtil.getInstance().getName() + " request", Messages.getQuestionIcon());
					pluginConfiguration.getGeneralConfigurationData().setAnonymousFeedbackEnabled(
							answer == DialogWrapper.OK_EXIT_CODE);
				}
			}
		}, ModalityState.defaultModalityState());
	}

	public void projectClosed() {
		if (created) {
			// remove icon from status bar
			statusBarBambooIcon.hideIcon();
			statusBarBambooIcon = null;
			statusBarCrucibleIcon.hideIcon();
			statusBarCrucibleIcon = null;
			statusPluginUpdateIcon.hideIcon();
			statusPluginUpdateIcon = null;

			IdeaHelper.getAppComponent().getSchedulableCheckers().remove(bambooStatusChecker);
			IdeaHelper.getAppComponent().getSchedulableCheckers().remove(crucibleStatusChecker);
			IdeaHelper.getAppComponent().rescheduleStatusCheckers(true);
			// unregister listeners
			//bambooStatusChecker.unregisterListener(iconBambooStatusListener);
			//bambooStatusChecker.unregisterListener(toolWindowBambooListener);
			bambooStatusChecker.unregisterListener(tooltipBambooStatusListener);
			//unregister form model
			//crucibleStatusChecker.unregisterListener(crucibleReviewNotifier);
			cfgManager.removeProjectConfigurationListener(CfgUtil.getProjectId(project), configurationListener);
			configurationListener = null;
			cfgManager.removeProjectConfigurationListener(CfgUtil.getProjectId(project),
					issuesToolWindowPanel.getConfigListener());

			// remove tool window
			toolWindowManager.unregisterToolWindow(PluginToolWindow.TOOL_WINDOW_NAME);

			EditorFactory.getInstance().removeEditorFactoryListener(crucibleEditorFactoryListener);

			//remove Crucible listeners
			crucibleReviewNotifier.unregisterListener(crucibleTooltip);
			crucibleReviewListModel.removeListener(crucibleReviewNotifier);

			fileEditorListener.projectClosed();
			created = false;
		}
	}

	public ProjectConfigurationBean getProjectConfigurationBean() {
		return projectConfigurationBean;
	}

	public CrucibleStatusChecker getCrucibleStatusChecker() {
		return crucibleStatusChecker;
	}

	public BambooStatusChecker getBambooStatusChecker() {
		return bambooStatusChecker;
	}

	private class ConfigurationListenerImpl extends ConfigurationListenerAdapter {

		@Override
		public void configurationUpdated(final ProjectConfiguration aProjectConfiguration) {
			// show-hide icons if necessary
			statusBarBambooIcon.showOrHideIcon();
			statusBarCrucibleIcon.showOrHideIcon();
			// show-hide panels if necessary
			toolWindow.showHidePanels();
		}
	}
}

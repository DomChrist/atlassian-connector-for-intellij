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
package com.atlassian.theplugin.idea.config;

import com.atlassian.connector.cfg.ProjectCfgManager;
import com.atlassian.connector.intellij.bamboo.IntelliJBambooServerFacade;
import com.atlassian.connector.intellij.fisheye.IntelliJFishEyeServerFacade;
import com.atlassian.theplugin.commons.ServerType;
import com.atlassian.theplugin.commons.UiTaskExecutor;
import com.atlassian.theplugin.commons.bamboo.BambooServerData;
import com.atlassian.theplugin.commons.cfg.BambooServerCfg;
import com.atlassian.theplugin.commons.cfg.ConfigurationListenerAdapter;
import com.atlassian.theplugin.commons.cfg.JiraServerCfg;
import com.atlassian.theplugin.commons.cfg.PrivateConfigurationDao;
import com.atlassian.theplugin.commons.cfg.PrivateProjectConfiguration;
import com.atlassian.theplugin.commons.cfg.PrivateServerCfgInfo;
import com.atlassian.theplugin.commons.cfg.ProjectConfiguration;
import com.atlassian.theplugin.commons.cfg.ServerCfg;
import com.atlassian.theplugin.commons.cfg.ServerCfgFactoryException;
import com.atlassian.theplugin.commons.cfg.ServerIdImpl;
import com.atlassian.theplugin.commons.cfg.xstream.JDomProjectConfigurationDao;
import com.atlassian.theplugin.commons.cfg.xstream.UserSharedConfigurationDao;
import com.atlassian.theplugin.commons.jira.IntelliJJiraServerFacade;
import com.atlassian.theplugin.commons.jira.JiraServerData;
import com.atlassian.theplugin.commons.remoteapi.ServerData;
import com.atlassian.theplugin.configuration.WorkspaceConfigurationBean;
import com.atlassian.theplugin.idea.IdeaHelper;
import com.atlassian.theplugin.idea.IdeaVersionFacade;
import com.atlassian.theplugin.idea.ui.DialogWithDetails;
import com.atlassian.theplugin.util.PluginUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.SettingsSavingComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ProjectConfigurationComponent implements ProjectComponent, SettingsSavingComponent, Configurable {

	private final Project project;
    private final ProjectManager projectManager;
    private final ProjectCfgManagerImpl projectCfgManager;
	private final UiTaskExecutor uiTaskExecutor;
	private final PrivateConfigurationDao privateCfgDao;
    private final UserSharedConfigurationDao userSharedConfigurationDao;
	private final WorkspaceConfigurationBean projectConfigurationBean;
	private static final String CFG_LOAD_ERROR_MSG =
			"Error while loading the configuration of " + PluginUtil.PRODUCT_NAME;
	private static final Icon PLUGIN_SETTINGS_ICON = IconLoader.getIcon("/icons/ico_plugin.png");
	private ProjectConfigurationPanel projectConfigurationPanel;
	private final LocalConfigurationListener configurationListener = new LocalConfigurationListener();
	/**
	 * race condtions wrt to this variable are harmless as threads mutating it (via save) reset it from false to true
	 * and then save configuration. So saving (if really needed) will not be skipped anyway even if two threads
	 * start in the moment when shouldSaveConfiguration was false
	 */
	private boolean shouldSaveConfiguration;
	private ServerData selectedServer;


	public ProjectConfigurationComponent(final Project project, final ProjectManager projectManager, final ProjectCfgManager projectCfgManager,
			final UiTaskExecutor uiTaskExecutor,
			@NotNull PrivateConfigurationDao privateCfgDao,
            @NotNull WorkspaceConfigurationBean projectConfigurationBean,
            @NotNull UserSharedConfigurationDao sharedCfgDao) {
		this.project = project;
        this.projectManager = projectManager;
        this.projectCfgManager = (ProjectCfgManagerImpl) projectCfgManager;
		this.uiTaskExecutor = uiTaskExecutor;
		this.privateCfgDao = privateCfgDao;
        this.userSharedConfigurationDao = sharedCfgDao;
		this.projectConfigurationBean = projectConfigurationBean;
//		shouldSaveConfiguration = load();
	}


	public static void handleServerCfgFactoryException(final Project theProject, final Exception e) {
		ApplicationManager.getApplication().invokeLater(new Runnable() {
			public void run() {
				DialogWithDetails.showExceptionDialog(theProject, CFG_LOAD_ERROR_MSG + "\nEmpty configuration will be used.\n"
						+ "If you want to preserve settings stored in your configuration, do not edit your "
						+ PluginUtil.PRODUCT_NAME + " configuration using IDEA, but instead close the project "
						+ "and try resolve the problem by modyfing directly the configuration file", e);
			}
		});
	}

	public void projectOpened() {
        shouldSaveConfiguration = load();
		if (projectCfgManager.getProjectConfiguration() == null) {
			ApplicationManager.getApplication().invokeLater(new Runnable() {
				public void run() {
					Messages.showErrorDialog(project, "If you see this message, something bad happend to the "
							+ "initialization sequence of IDEA. You may encounter now various strange problems with Connector."
							+ "\nPlease report occurence of this message to us.",
							"Internal Error in Atlassian Connector");
				}
			});
		}

		projectCfgManager.addProjectConfigurationListener(configurationListener);
	}


	public void projectClosed() {
		projectCfgManager.removeProjectConfigurationListener(configurationListener);
	}

	@NonNls
	@NotNull
	public String getComponentName() {
		return ProjectConfigurationComponent.class.getSimpleName();
	}

	public void initComponent() {
	}

	public void disposeComponent() {
	}


	public boolean load() {
		migrateFileFromProjectRootToIdeaDirectory();

        Element rootElement = new Element("atlassian-ide-plugin");
        Element pc = new Element("project-configuration");
        pc.getChildren().add(new Element("servers"));
        rootElement.getChildren().add(pc);
        Document root = new Document(rootElement);
		final SAXBuilder builder = new SAXBuilder(false);
//		final ProjectId projectId = CfgUtil.getProjectId(project);
		try {
			final String path = getCfgFilePath();
			final File file = path != null ? new File(path) : null;
			if (path == null || !file.exists()) {
				// this is an empty project (default template used by IDEA)
				setDefaultProjectConfiguration();
			} else {
                FileInputStream inStream = new FileInputStream(file);
                root = builder.build(inStream);
                cleanupDom(root);
            }
		} catch (Exception e) {
			handleServerCfgFactoryException(project, e);
			setDefaultProjectConfiguration();
			return false;
		}

		final JDomProjectConfigurationDao cfgFactory =
                new JDomProjectConfigurationDao(root.getRootElement(), privateCfgDao, userSharedConfigurationDao);
		migrateOldPrivateProjectSettings(cfgFactory);

		try {
			final ProjectConfiguration projectConfiguration;
			projectConfiguration = cfgFactory.load();

            PasswordStorage.loadPasswordsFromSecureStore(project, null /*projectManager.getDefaultProject()*/, projectConfiguration, cfgFactory);

			if (projectConfiguration.getDefaultFishEyeServer() == null) {
				//means that configuration holds Crucible as FishEye server.
				//in the future this code should be removed
				//now resolves migration problem from Crucible as FishEye to pure FishEye
				projectConfiguration.setDefaultFishEyeServerId(null);
			}
			projectCfgManager.updateProjectConfiguration(projectConfiguration);
		} catch (ServerCfgFactoryException e) {
			handleServerCfgFactoryException(project, e);
			setDefaultProjectConfiguration();
			return false;
		}


		return true;
	}

	private void migrateOldPrivateProjectSettings(final JDomProjectConfigurationDao cfgFactory) {
		final SAXBuilder builder = new SAXBuilder(false);
		final File privateCfgFile = getPrivateOldCfgFilePath();
		boolean someMigrationHappened = false;
		if (privateCfgFile != null) {
			try {
				final Document privateRoot = builder.build(privateCfgFile);
				final PrivateProjectConfiguration ppc = cfgFactory.loadOldPrivateConfiguration(privateRoot.getRootElement());
				for (PrivateServerCfgInfo privateServerCfgInfo : ppc.getPrivateServerCfgInfos()) {
					try {
						final PrivateServerCfgInfo newPsci = privateCfgDao.load(privateServerCfgInfo.getServerId());
						if (newPsci == null) {
							privateCfgDao.save(privateServerCfgInfo);
							someMigrationHappened = true;
						}
					} catch (ServerCfgFactoryException e) {
						// ignore here - just don't try to overwrite it with data from old XML file
					}
				}
			} catch (Exception e) {
				handleServerCfgFactoryException(project, e);
			}
		}



		if (someMigrationHappened) {
			ApplicationManager.getApplication().invokeLater(new Runnable() {
				public void run() {
					int value = Messages.showYesNoCancelDialog(project,
							"Configuration has been succesfully migrated to new location (home directory).\n"
									+ "Would you like to delete the old configuration file?\n\nDelete file: ["
									+ privateCfgFile + "]",
							PluginUtil.PRODUCT_NAME + " upgrade process", Messages.getQuestionIcon());

					if (value == DialogWrapper.OK_EXIT_CODE) {
						if (!privateCfgFile.delete()) {
							Messages.showWarningDialog(project, "Cannot remove file [" + privateCfgFile.getAbsolutePath()
									+ "].\nTry removing it manually.\n" + PluginUtil.PRODUCT_NAME
									+ " should still behave correctly.", PluginUtil.PRODUCT_NAME);
						}

					}
				}
			});
		}
	}

	private void migrateFileFromProjectRootToIdeaDirectory() {
		boolean migrationHappened = false;

		final File cfgFileInProjectRoot = new File(getCfgFilePathInProjectRootDirectory());
		final File newCfgFile = new File(getCfgFilePath());

        // note that this will always be false when using file-based project configuration, as both paths are equal
		if (cfgFileInProjectRoot.exists() && !newCfgFile.exists()) {
			try {
				FileUtils.moveFile(cfgFileInProjectRoot, newCfgFile);
				migrationHappened = true;
			} catch (IOException e) {
				handleServerCfgFactoryException(project, e);
			}
		}

		if (migrationHappened) {
			ApplicationManager.getApplication().invokeLater(new Runnable() {
				public void run() {
					Messages.showInfoMessage(project,
							"The Atlassian IDE Connector configuration file has been moved from "
								+ getCfgFilePathInProjectRootDirectory() + " to "
								+ getCfgFilePath() + ". If it was under version control, please make "
								+ "the appropriate changes to reflect this.",
							PluginUtil.PRODUCT_NAME + " upgrade process");
				}
			});
		}
	}

	/**
	 * Ensuring that old attributes do not break our loading
	 *
	 * @param root root element of XML document
	 * @throws org.jdom.JDOMException when tree is invalid
	 */
	private void cleanupDom(final Document root) throws JDOMException {
		@SuppressWarnings("unchecked")
		final List<Element> prjCfgNode = XPath.selectNodes(root, "atlassian-ide-plugin/project-configuration");
		for (Element e : prjCfgNode) {
			e.removeChild("isPrivateConfigurationMigrated");
			e.removeChild("defaultJiraServerId");
			e.removeChild("defaultJiraProject");
		}
		/** there was defaultUser (by mistake) child of server element */
		@SuppressWarnings("unchecked")
		List<Element> serverNodes = XPath.selectNodes(root, "atlassian-ide-plugin/project-configuration/servers/*");
		for (Element e : serverNodes) {
			e.removeChild("defaultUser");
		}

		@SuppressWarnings("unchecked")
		List<Element> projectConfigurationNode = XPath.selectNodes(root, "atlassian-ide-plugin/project-configuration");
		for (Element e : projectConfigurationNode) {
			e.removeChild("defaultUser");
		}
		
		/* remove unused anymore BambooServerCfg fields */
		@SuppressWarnings("unchecked")
		List<Element> bambooNodes = XPath.selectNodes(root, "atlassian-ide-plugin/project-configuration/servers/bamboo");
		for (Element e : bambooNodes) {
			e.removeChild("isBamboo2M9");
			e.removeChild("isBamboo24");
		}
	}

	private ProjectConfiguration setDefaultProjectConfiguration() {
		final ProjectConfiguration configuration = ProjectConfiguration.emptyConfiguration();
		projectCfgManager.updateProjectConfiguration(configuration);
		return configuration;
	}

	@Nullable
	private String getCfgFilePathInProjectRootDirectory() {
		final VirtualFile baseDir = project.getBaseDir();
		if (baseDir == null) {
			return null;
		}
		return baseDir.getPath() + File.separator + "atlassian-ide-plugin.xml";
	}

	@Nullable
	private String getCfgFilePath() {
		final VirtualFile baseDir = project.getBaseDir();
		if (baseDir == null) {
			return null;
		}
        File dotIdeaDirectory = new File(baseDir.getPath() + File.separator + ".idea");
        if (dotIdeaDirectory.exists() && dotIdeaDirectory.isDirectory()) {
            return baseDir.getPath() + File.separator + ".idea" + File.separator + "atlassian-ide-plugin.xml";
        } else {
            return getCfgFilePathInProjectRootDirectory();
        }
	}


	private File getPrivateOldCfgFilePath() {
		final VirtualFile baseDir = project.getBaseDir();
		if (baseDir == null) {
			return null;
		}
		final File baseNewProjectFile = new File(baseDir.getPath()
				+ File.separator + "atlassian-ide-plugin.private.xml");

		if (baseNewProjectFile.isFile() && baseNewProjectFile.canRead()) {
			return baseNewProjectFile;
		}

		return null;
	}

//	private ProjectId getProjectId() {
//		return CfgUtil.getProjectId(project);
//	}

	public void save() {
		final Element element = new Element("atlassian-ide-plugin");

		JDomProjectConfigurationDao cfgFactory =
                new JDomProjectConfigurationDao(element, privateCfgDao, userSharedConfigurationDao);
		final ProjectConfiguration configuration = projectCfgManager.getProjectConfiguration();
		if (configuration != null) {
			if (configuration.getServers().size() > 0 && !shouldSaveConfiguration) {
				// apparently somebody still prefers to populate invalid configuration, so we would save it now
				shouldSaveConfiguration = true;
			}

            if (shouldSaveConfiguration) {

                PasswordStorage.savePasswordsToSecureStore(project, null  /*projectManager.getDefaultProject()*/, configuration, cfgFactory);
                
				final String publicCfgFile = getCfgFilePath();

				writeXmlFile(element, publicCfgFile);

			}
		}
	}

	private void writeXmlFile(final Element element, final String filepath) {
		if (filepath == null) {
			return; // handlig for instance default dummy project
		}
		try {
			final FileWriter writer = new FileWriter(filepath);

            final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            //outputter.setLineSeparator(System.getProperty("line.separator"));
            outputter.output(element, writer);
			writer.close();
		} catch (IOException e) {
			Messages.showWarningDialog(project, "Cannot save project configuration settings to [" + filepath + ":\n"
					+ e.getMessage(), "Error");
		}
	}

    @Nls
    public String getDisplayName() {
        return IdeaVersionFacade.getInstance().getSettingsMenuProjectDisplayString();
    }

	@Nullable
	public Icon getIcon() {
		return PLUGIN_SETTINGS_ICON;
	}

	@Nullable
	@NonNls
	public String getHelpTopic() {
		return null;
	}

	public JComponent createComponent() {
		ProjectConfiguration configuration = projectCfgManager.getProjectConfiguration();
		if (configuration == null) {
			// may happen for Default Template project
			configuration = setDefaultProjectConfiguration();
		}


		projectConfigurationPanel =
				new ProjectConfigurationPanel(project, configuration.getClone(),
						IntelliJFishEyeServerFacade.getInstance(),
						IntelliJBambooServerFacade.getInstance(PluginUtil.getLogger()),
						IntelliJJiraServerFacade.getInstance(), uiTaskExecutor, selectedServer, projectCfgManager
								.getDefaultCredentials().getClone(),
				projectCfgManager.isDefaultCredentialsAsked(), projectConfigurationBean);
		return projectConfigurationPanel;
	}

	public boolean isModified() {
		projectConfigurationPanel.saveData(false);
		return !(projectCfgManager.getProjectConfiguration().equals(projectConfigurationPanel.getProjectConfiguration())
				&& projectCfgManager.getDefaultCredentials().equals(projectConfigurationPanel.getDefaultCredentials())
				&& projectCfgManager.isDefaultCredentialsAsked() == projectConfigurationPanel.isDefaultCredentialsAsked());
	}

	public void apply() throws ConfigurationException {
		projectConfigurationPanel.askForDefaultCredentials();
		if (projectConfigurationPanel == null) {
			return;
		}
		projectConfigurationPanel.saveData(true);
		projectCfgManager.updateProjectConfiguration(projectConfigurationPanel.getProjectConfiguration());
		projectConfigurationPanel.setData(projectCfgManager.getProjectConfiguration().getClone());
		projectCfgManager.setDefaultCredentials(projectConfigurationPanel.getDefaultCredentials());
		projectCfgManager.setDefaultCredentialsAsked(projectConfigurationPanel.isDefaultCredentialsAsked());
	}

	public void updateConfiguration(final ProjectConfiguration projectConfiguration) {
		projectCfgManager.updateProjectConfiguration(projectConfiguration);
		if (projectConfigurationPanel != null) {
			projectConfigurationPanel.setData(projectCfgManager.getProjectConfiguration().getClone());
		}
	}

	public ProjectConfiguration getProjectConfigurationClone() {
		return projectCfgManager.getProjectConfiguration().getClone();
	}


	// pstefaniak, 21 jan 2010: this should probably go outside of this class... to some kind of helper class... dunno
	public static void fireDirectClickedServerPopup(final Project project, final String serverUrl, final ServerType serverType,
			final Runnable runnable) {
/*		final Color BACKGROUND_COLOR = new Color(255, 255, 200);

		StringBuilder sb = new StringBuilder("Server <i>" + serverUrl + "</i> not found in configuration<br>");
		sb.append("<br>Click on this notification to open configuration panel and add this server");

		JEditorPane content = new JEditorPane();
		content.setEditable(false);
		content.setContentType("text/html");
		content.setEditorKit(new ClasspathHTMLEditorKit());
		content.setText("<html>" + Constants.BODY_WITH_STYLE + sb.toString() + "</body></html>");
		content.setBackground(BACKGROUND_COLOR);
		content.addHyperlinkListener(new GenericHyperlinkListener());

		content.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (ProjectConfigurationComponent.addDirectClickedServer(project, serverUrl, serverType)) {
					EventQueue.invokeLater(runnable);
				}
			}
		});

		IdeaVersionFacade.getInstance().fireNotification(
				project,
				new JScrollPane(content),
				content.getText(),
				"/icons/crucible-blue-16.png",
				IdeaVersionFacade.OperationStatus.INFO,
				BACKGROUND_COLOR);*/

		if (Messages.showYesNoDialog("Server " + serverUrl + " not found in configuration,\ndo you want to "
				+ "open configuration panel and add this server?",
				"Server not found",
				Messages.getQuestionIcon()) == DialogWrapper.OK_EXIT_CODE) {
			if (ProjectConfigurationComponent.addDirectClickedServer(project, serverUrl, serverType)) {
				EventQueue.invokeLater(runnable);
			}
		}
	}

	/*
	 * Method for adding auto-filled server configuration based on parameters passed by directClickThroughRequest
	 *
	 * @return true if user clicked accept, false if clicked cancel
	 * @remark pstefaniak, 21 jan 2010: this should probably go outside of this class... to some kind of helper class... dunno
	 */
	public static boolean addDirectClickedServer(final Project project, final String serverUrl, ServerType serverType) {
		ProjectConfigurationComponent component = project.getComponent(ProjectConfigurationComponent.class);

		ProjectConfiguration configurationClone = component.getProjectConfigurationClone();

		ServerIdImpl id = new ServerIdImpl();
		String name = serverUrl;
		if (name.contains("://")) {
			name = name.substring(name.indexOf("://") + 3);
			if (name.contains("/")) {
				name = name.substring(0, name.indexOf("/"));
			}
		}
		ServerCfg serverCfg = null;
        final ServerData data;
		//beautiful switch: :/
		switch (serverType) {
			case BAMBOO_SERVER:
				serverCfg = new BambooServerCfg(name, id);
                data =  new BambooServerData((BambooServerCfg)serverCfg);
				break;
			case JIRA_SERVER:
				serverCfg = new JiraServerCfg(name, id, true);
                data =  new JiraServerData(serverCfg);
				break;
			default:
				throw new AssertionError("switch not implemented for [" + serverType + "]");
		}

		serverCfg.setUrl(serverUrl);
		serverCfg.setUseDefaultCredentials(true);

		configurationClone.getServers().add(serverCfg);
		component.updateConfiguration(configurationClone);

        component.setSelectedServer(data);

		final ShowSettingsUtil settingsUtil = ShowSettingsUtil.getInstance();
		if (settingsUtil != null) {
			boolean hasClickedOkButton = settingsUtil.editConfigurable(project, component);
			if (!hasClickedOkButton) {
				configurationClone = component.getProjectConfigurationClone();
				ServerCfg toRemoveCfg = configurationClone.getServerCfg(id);
				configurationClone.getServers().remove(toRemoveCfg);
				component.updateConfiguration(configurationClone);
			}
			return hasClickedOkButton;
		}
		return false;
	}

	public void reset() {
	}

	public void disposeUIResources() {
		projectConfigurationPanel = null;
	}

	public void setSelectedServer(final ServerData server) {
		this.selectedServer = server;
	}

	private class LocalConfigurationListener extends ConfigurationListenerAdapter {
		@Override
		public void configurationUpdated(ProjectConfiguration aProjectConfiguration) {
			save();
			IdeaHelper.getAppComponent().rescheduleStatusCheckers(true);
		}
	}
}

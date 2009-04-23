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
package com.atlassian.theplugin.idea.action.issues.activetoolbar;

import com.atlassian.theplugin.cache.RecentlyOpenIssuesCache;
import com.atlassian.theplugin.idea.IdeaHelper;
import com.atlassian.theplugin.idea.jira.CachedIconLoader;
import com.atlassian.theplugin.jira.api.JIRAIssue;
import com.atlassian.theplugin.jira.model.ActiveJiraIssue;
import com.atlassian.theplugin.jira.model.ActiveJiraIssueBean;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import javax.swing.*;

/**
 * User: pmaruszak
 */
public class ActiveJiraIssueComboBox extends ComboBoxAction {
	static final Icon JIRA_ICON = IconLoader.getIcon("/icons/jira-blue-16.png");
	static final Icon JIRA_ICON_DISABLED = IconLoader.getIcon("/icons/jira-grey-16.png");
	private static final int SUMMARY_LENGHT = 50;

	public void update(final AnActionEvent event) {
		ActiveJiraIssue activeIssue = ActiveIssueUtils.getActiveJiraIssue(event);
		String text = "No active issue";
		String summary = "";
		JIRAIssue issue = null;

		if (activeIssue != null) {
			text = activeIssue.getIssueKey();

			RecentlyOpenIssuesCache cache = IdeaHelper.getProjectComponent(event, RecentlyOpenIssuesCache.class);
			if (cache != null) {
				issue = cache.getLoadedRecenltyOpenIssue(activeIssue.getIssueKey(), activeIssue.getServerId());

				if (issue != null) {
					summary = issue.getSummary();
					if (summary.length() > SUMMARY_LENGHT) {
						summary = summary.substring(0, SUMMARY_LENGHT) + "...";
					}
				}
			}
		}

		event.getPresentation().setText(text);
		event.getPresentation().setDescription(text + ": " + summary);

		if (issue != null) {
			event.getPresentation().setIcon(CachedIconLoader.getIcon(issue.getTypeIconUrl()));
		}
		super.update(event);
	}

	@NotNull
	protected DefaultActionGroup createPopupActionGroup(final JComponent jComponent) {
		final Project project = IdeaHelper
				.getCurrentProject(DataManager.getInstance().getDataContext(jComponent));


		DefaultActionGroup group = new DefaultActionGroup("Issues to activate", true);
//		final JiraWorkspaceConfiguration conf = IdeaHelper .getProjectComponent(project, JiraWorkspaceConfiguration.class);
		final RecentlyOpenIssuesCache cache = IdeaHelper.getProjectComponent(project, RecentlyOpenIssuesCache.class);
		final ActiveJiraIssue activeIssue = ActiveIssueUtils.getActiveJiraIssue(project);
		if (cache != null) {
			for (JIRAIssue issue : cache.getLoadedRecenltyOpenIssues()) {
				if (activeIssue == null || !issue.getKey().equals(activeIssue.getIssueKey())) {
					ActiveJiraIssue newActiveIsse = new ActiveJiraIssueBean(issue.getServer().getServerId().toString(),
							issue.getKey(), new DateTime());
					group.add(new ActivateIssueItemAction(newActiveIsse, project));
				}
			}
		}

		return group;
	}
}

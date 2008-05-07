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

package com.atlassian.theplugin.idea.action.jira;

import com.atlassian.theplugin.idea.IdeaHelper;
import com.atlassian.theplugin.idea.PluginToolWindow;
import com.atlassian.theplugin.idea.jira.IssueComment;
import com.atlassian.theplugin.idea.jira.JIRAToolWindowPanel;
import com.atlassian.theplugin.idea.jira.JiraIssueAdapter;
import com.atlassian.theplugin.jira.JIRAServer;
import com.atlassian.theplugin.jira.api.JIRAException;
import com.atlassian.theplugin.jira.api.JIRAIssue;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.util.List;

public class CommentIssueAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
		IdeaHelper.getJIRAToolWindowPanel(e).addCommentToIssue();
    }

	public void update(AnActionEvent event) {
		super.update(event);
		if (IdeaHelper.getCurrentJIRAServer() != null) {
			event.getPresentation().setEnabled(IdeaHelper.getJIRAToolWindowPanel(event).getCurrentIssue() != null);
		} else {
			event.getPresentation().setEnabled(false);
		}
	}
}

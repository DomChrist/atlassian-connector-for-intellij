package com.atlassian.theplugin.idea.action.issues;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.atlassian.theplugin.idea.jira.IssuesToolWindowPanel;
import com.atlassian.theplugin.idea.IdeaHelper;
import com.atlassian.theplugin.commons.cfg.JiraServerCfg;
import com.atlassian.theplugin.cfg.CfgUtil;
import com.atlassian.theplugin.jira.api.JIRAException;

import java.util.Collection;

/**
 * User: pmaruszak
 */
public class RefreshIssuesAction extends AnAction {
	public void actionPerformed(final AnActionEvent e) {
		final IssuesToolWindowPanel panel = IdeaHelper.getIssuesToolWindowPanel(e);
		panel.refreshIssues();
	}
}

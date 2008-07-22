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

package com.atlassian.theplugin.idea.action.jira.editor;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ide.BrowserUtil;
import com.atlassian.theplugin.idea.jira.editor.ThePluginJIRAEditorComponent;
import com.atlassian.theplugin.jira.api.JIRAIssue;

public class EditorEditIssueAction extends AnAction {
	public void actionPerformed(AnActionEvent event) {
		ThePluginJIRAEditorComponent.JIRAFileEditor editor =
				ThePluginJIRAEditorComponent.getEditorByKey(event.getPlace());
		if (editor != null) {
			JIRAIssue issue = editor.getIssue();
			BrowserUtil.launchBrowser(issue.getServerUrl()
            	+ "/secure/EditIssue!default.jspa?key=" + issue.getKey());
		}
	}
}

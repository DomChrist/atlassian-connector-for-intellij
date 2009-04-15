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
package com.atlassian.theplugin.idea.ui.linkhiglighter;

import com.atlassian.theplugin.cfg.CfgUtil;
import com.atlassian.theplugin.commons.cfg.JiraServerCfg;
import com.atlassian.theplugin.idea.IdeaHelper;
import com.atlassian.theplugin.idea.jira.IssuesToolWindowPanel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * User: pmaruszak
 */
class EditorInputHandler extends KeyAdapter implements EditorMouseMotionListener, EditorMouseListener {
	private Point lastPointLocation = null;
	private final Project project;
	private final Editor editor;
	private final PsiFile file;
	private final JiraEditorLinkParser jiraEditorLinkParser;
	private boolean handCursor;

	public EditorInputHandler(@NotNull Project project, @NotNull Editor editor, PsiFile file,
			JiraEditorLinkParser jiraEditorLinkParser) {
		this.project = project;

		this.editor = editor;
		this.file = file;
		this.jiraEditorLinkParser = jiraEditorLinkParser;
	}

	public void mouseMoved(final EditorMouseEvent event) {
		lastPointLocation = event.getMouseEvent().getPoint();
		updateCursor();
	}

	public void mouseDragged(final EditorMouseEvent event) {
		updateCursor();
	}

	public void keyPressed(final KeyEvent e) {
		updateCursor();
	}

	public void keyReleased(final KeyEvent e) {
		updateCursor();
	}


	private void updateCursor() {
		JiraURLTextRange hoverRange = jiraEditorLinkParser.getJiraURLTextRange(editor, file, lastPointLocation);
		if (hoverRange != null && hoverRange.isActive()) {
			editor.getContentComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			handCursor = true;
		} else if (handCursor) {
			editor.getContentComponent().setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
			handCursor = false;
		}

	}

	public void mouseClicked(final EditorMouseEvent event) {
		if (event.getMouseEvent().getClickCount() == 1) {
			JiraURLTextRange hoverRange =
					jiraEditorLinkParser.getJiraURLTextRange(editor, file, event.getMouseEvent().getPoint());
			if (hoverRange != null && hoverRange.isActive()) {
				IssuesToolWindowPanel panel = IdeaHelper.getIssuesToolWindowPanel(project);
				JiraServerCfg defaultJiraServer =
						IdeaHelper.getCfgManager().getProjectConfiguration(CfgUtil.getProjectId(project))
								.getDefaultJiraServer();
				panel.openIssue(hoverRange.getIssueKey(), defaultJiraServer);
			}
		}

	}

	public void mousePressed(final EditorMouseEvent editorMouseEvent) {
	}

	public void mouseReleased(final EditorMouseEvent editorMouseEvent) {
	}

	public void mouseEntered(final EditorMouseEvent editorMouseEvent) {
	}

	public void mouseExited(final EditorMouseEvent editorMouseEvent) {
	}
}
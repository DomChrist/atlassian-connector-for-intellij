package com.atlassian.theplugin.idea.action.bamboo.changes;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.atlassian.theplugin.idea.bamboo.BuildChangesToolWindow;

public class GroupByDirectoryAction extends ToggleAction {
	public boolean isSelected(AnActionEvent event) {
		BuildChangesToolWindow.ChangesTree tree = BuildChangesToolWindow.getChangesTree(event.getPlace());
		if (tree == null) {
			return !BuildChangesToolWindow.ChangesTree.GROUP_BY_DIRECTORY_DEFAULT;
		}
		return tree.isGroupByDirectory();
	}

	public void setSelected(AnActionEvent event, boolean b) {
		BuildChangesToolWindow.ChangesTree tree = BuildChangesToolWindow.getChangesTree(event.getPlace());
		tree.setGroupByDirectory(b);
	}
}

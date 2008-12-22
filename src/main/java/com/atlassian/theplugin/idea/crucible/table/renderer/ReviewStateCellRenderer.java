package com.atlassian.theplugin.idea.crucible.table.renderer;

import com.atlassian.theplugin.commons.crucible.api.model.ReviewAdapter;

public class ReviewStateCellRenderer extends ReviewCellRenderer {
	protected String getCellText(ReviewAdapter review) {
		return review.getState().value();
	}

	protected String getCellToolTipText(ReviewAdapter review) {
		return null;
	}
}

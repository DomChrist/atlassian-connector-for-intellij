package com.atlassian.theplugin.jira.api.fields;

import com.atlassian.theplugin.jira.api.JIRAIssue;
import com.atlassian.theplugin.jira.api.soap.axisv4.RemoteIssue;

import java.util.List;
import java.util.ArrayList;

/**
 * User: jgorycki
 * Date: Apr 6, 2009
 * Time: 4:55:19 PM
 */
public class ResolutionFiller implements FieldFiller {
	public List<String> getFieldValues(String field, JIRAIssue detailedIssue) {
		RemoteIssue ri = (RemoteIssue) detailedIssue.getRawSoapIssue();
		if (ri == null) {
			return null;
		}
		List<String> result = new ArrayList<String>();
		result.add(ri.getResolution());
		return result;
	}
}
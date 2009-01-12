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

import junit.framework.TestCase;

public class HelpUrlTest extends TestCase {
	public void testGetGoodHelpTopic() {
		String s = HelpUrl.getHelpUrl(Constants.HELP_CONFIG_PANEL);
		assertNotNull(s);
		assertTrue(s.startsWith("http://confluence.atlassian.com"));
		assertTrue(s.contains("IDEPLUGIN"));
	}

	public void testGetBadHelpTopic() {
		assertNull(HelpUrl.getHelpUrl("dupa"));
	}
}
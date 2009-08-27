package com.jetbrains.teamcity.vcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;


public class PerforceSupportTest {
	
	@Test
	public void findPerforceRoot_null() throws Exception {
		//single mapping
		final String mapping = null;
		final String root = PerforceSupport.findPerforceRoot(mapping);
		assertNull(root);
	}
	
	@Test
	public void findPerforceRoot_empty() throws Exception {
		//single mapping
		final String mapping = "";
		final String root = PerforceSupport.findPerforceRoot(mapping);
		assertNull(root);
	}
	
	@Test
	public void findPerforceRoot_wrong_line() throws Exception {
		//single mapping
		final String mapping = "aaaaaa";
		final String root = PerforceSupport.findPerforceRoot(mapping);
		assertNull(root);
	}
	
	@Test
	public void findPerforceRoot_wrong_line2() throws Exception {
		//single mapping
		final String mapping = "aaaaaa aasaasa";
		final String root = PerforceSupport.findPerforceRoot(mapping);
		assertNull(root);
	}
	
	@Test
	public void findPerforceRoot_single() throws Exception {
		//single mapping
		final String mapping = "\n//repo/test/...    //team-city-agent/...\n";
		final String root = PerforceSupport.findPerforceRoot(mapping);
		assertNotNull(root);
		assertEquals("//repo/test", root);
	}
	
	@Test
	public void findPerforceRoot_multiple_with_root() throws Exception {
		//multiple mapping with root
		final String mapping = "\n//repo/test/...  //team-city-agent/...\n" +
							   "//repo/dev/src/... //team-city-agent/src/...  \n";
		final String root = PerforceSupport.findPerforceRoot(mapping);
		assertNotNull(root);
		assertEquals("//repo/test", root);
	}
	
	@Test
	public void findPerforceRoot_multiple_with_single_agent_root() throws Exception {
		//multiple mapping with root
		final String mapping = "//repo/test/...    //team-city-agent/a/...\n" +
							   "//repo/dev/src/... //team-city-agent-a/src/...  ";
		final String root = PerforceSupport.findPerforceRoot(mapping);
		assertNotNull(root);
		assertEquals("//repo/test", root);
	}

	@Test
	public void findPerforceRoot_multiple_without_root() throws Exception {
		//multiple mapping with root
		final String mapping = "//repo/test/...    //team-city-agent/a/...\n" +
				               "//repo/dev/src/... //team-city-agent/src/...  ";
		final String root = PerforceSupport.findPerforceRoot(mapping);
		assertNull(root);
	}
	
	
}

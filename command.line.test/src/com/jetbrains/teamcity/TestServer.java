package com.jetbrains.teamcity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import jetbrains.buildServer.AddToQueueRequest;
import jetbrains.buildServer.AddToQueueResult;
import jetbrains.buildServer.BuildTypeData;
import jetbrains.buildServer.ProjectData;
import jetbrains.buildServer.TeamServerSummaryData;
import jetbrains.buildServer.BuildTypeDescriptor.CheckoutType;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.vcs.VcsRoot;

public class TestServer extends Server {
	
	public static final String TC_CONFIG_MULTIPLE_ROOT = "tc-config-multiple-root";
	public static final String TC_CONFIG_SINGLE_ROOT = "tc-config-single-root";
	public static final String TC_CONFIG_NO_ROOTS = "tc-config-no-one-vcs-roots";

	public static final String TC_PRJ_0 = "tc-prj-0";
	
	private HashMap<String, ProjectData> myProjects = new HashMap<String, ProjectData>();
	private HashMap<String, BuildTypeData> myConfigurations = new HashMap<String, BuildTypeData>();
	private HashMap<Long, VcsRoot> myRoots = new HashMap<Long, VcsRoot>();
	
	public TestServer(){
		super(null);
		//fill roots
		final TestVcsRoot root0 = new TestVcsRoot(0, "tc-vcs-0", new HashMap<String, String>());
		myRoots.put(root0.getId(), root0);
		final TestVcsRoot root1 = new TestVcsRoot(1, "tc-vcs-1", new HashMap<String, String>());
		myRoots.put(root1.getId(), root1);
		//fill projects
		final TestProjectData prj0 = new TestProjectData(TC_PRJ_0);
		myProjects.put(prj0.getProjectId(), prj0);
		//fill configurations
		final TestBuildTypeData cfg0 = new TestBuildTypeData(TC_CONFIG_NO_ROOTS, prj0, new ArrayList<VcsRoot>());
		myConfigurations.put(cfg0.getId(), cfg0);
		final TestBuildTypeData cfg1 = new TestBuildTypeData(TC_CONFIG_SINGLE_ROOT, prj0, new ArrayList<VcsRoot>(){{ add(root0);}});
		myConfigurations.put(cfg1.getId(), cfg1);
		final TestBuildTypeData cfg2 = new TestBuildTypeData(TC_CONFIG_MULTIPLE_ROOT, prj0, new ArrayList<VcsRoot>(){{ add(root0); add(root1);}});
		myConfigurations.put(cfg2.getId(), cfg2);
	}
	
	public static class TestVcsRoot implements VcsRoot {
		
		private long myId;
		private String myName;
		private Map<String, String> myProperties;

		public TestVcsRoot(long id, String name, Map<String, String> properties) {
			super();
			this.myId = id;
			this.myName = name;
			this.myProperties = properties;
		}

		public String convertToPresentableString() {
			return null;
		}

		public String convertToString() {
			return null;
		}

		public VcsRoot createSecureCopy() {
			return this;
		}

		public long getId() {
			return myId;
		}

		public String getName() {
			return myName;
		}

		public Map<String, String> getProperties() {
			return myProperties;
		}

		public long getPropertiesHash() {
			return myProperties.hashCode();
		}

		public String getProperty(String arg0) {
			return myProperties.get(arg0);
		}

		public String getProperty(String arg0, String arg1) {
			if(myProperties.containsKey(arg0)){
				return myProperties.get(arg0);
			}
			return arg1;
		}

		public long getRootVersion() {
			return 0;
		}

		public String getVcsName() {
			return "tc-test-vcs";
		}
		
	}
	
	public static class TestProjectData extends ProjectData {
		public TestProjectData(String id) {
			super(id, id, "<unknown>", Status.UNKNOWN, new ArrayList<BuildTypeData>(), new ArrayList<VcsRoot>());
		}
		
	}
	
	public static class  TestBuildTypeData extends BuildTypeData {

		public TestBuildTypeData(String id, ProjectData project, List<VcsRoot> roots) {
			super(false, id, project.getName(), project.getProjectId(), "<unknown>", id, "<unknown>",CheckoutType.MANUAL, Status.UNKNOWN, null, false, roots);
			project.getBuildTypes().add(this);
		}
		
	}

	@Override
	public AddToQueueResult addRemoteRunToQueue(ArrayList<AddToQueueRequest> batch, String myComments)
			throws ECommunicationException {
		// TODO Auto-generated method stub
		return super.addRemoteRunToQueue(batch, myComments);
	}

	@Override
	public void connect() throws ECommunicationException {
		//do nothing
	}

	@Override
	public Collection<String> getApplicableConfigurations(Collection<String> urls) {
		return super.getApplicableConfigurations(urls);
	}

	@Override
	public synchronized BuildTypeData getConfiguration(String id) throws ECommunicationException {
		return myConfigurations.get(id);
	}

	@Override
	public synchronized Collection<BuildTypeData> getConfigurations() throws ECommunicationException {
		return myConfigurations.values();
	}

	@Override
	public int getCurrentUser() {
		return -1;
	}

	@Override
	public synchronized Collection<ProjectData> getProjects() throws ECommunicationException {
		return myProjects.values();
	}

	@Override
	public TeamServerSummaryData getSummary() throws ECommunicationException {
		// TODO Auto-generated method stub
		return super.getSummary();
	}

	@Override
	public String getURL() {
		return "http://localhost";
	}

	@Override
	public synchronized VcsRoot getVcsRoot(long id) throws ECommunicationException {
		return myRoots.get(id);
	}

	@Override
	public synchronized Collection<VcsRoot> getVcsRoots() throws ECommunicationException {
		return myRoots.values();
	}

	@Override
	public void logon(String username, String password) throws ECommunicationException, EAuthorizationException {
		// do nothing
	}

	@Override
	public void logout() {
		// do nothing
	}
	
	@Test
	public void test_nothing() {
		//JUnit4 hack
	}
	
	
	
}

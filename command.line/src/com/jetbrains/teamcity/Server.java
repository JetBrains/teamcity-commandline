package com.jetbrains.teamcity;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import jetbrains.buildServer.AddToQueueRequest;
import jetbrains.buildServer.AddToQueueResult;
import jetbrains.buildServer.BuildTypeData;
import jetbrains.buildServer.IncompatiblePluginError;
import jetbrains.buildServer.ProjectData;
import jetbrains.buildServer.TeamServerSummaryData;
import jetbrains.buildServer.messages.XStreamHolder;
import jetbrains.buildServer.serverProxy.ApplicationFacade;
import jetbrains.buildServer.serverProxy.RemoteBuildServer;
import jetbrains.buildServer.serverProxy.RemoteBuildServerImpl;
import jetbrains.buildServer.serverProxy.VersionChecker;
import jetbrains.buildServer.serverProxy.impl.SessionXmlRpcTargetImpl;
import jetbrains.buildServer.serverSide.auth.AuthenticationFailedException;
import jetbrains.buildServer.vcs.VcsRoot;
import jetbrains.buildServer.xmlrpc.RemoteCallException;
import jetbrains.buildServer.xmlrpc.XmlRpcTarget.Cancelable;
import jetbrains.buildServer.xstream.ServerXStreamFormat;
import jetbrains.buildServer.xstream.XStreamWrapper;

import com.thoughtworks.xstream.XStream;

public class Server {

	private static final int CONNECTION_TIMEOUT = 10000;
	private URL myUrl;
	private SessionXmlRpcTargetImpl mySession;
	private Proxy myProxy;
	private RemoteBuildServer myServerProxy;
	private ArrayList<ProjectData> myProjects;

	public Server(final URL url) {
		myUrl = url;
	}
	
	public Server(final URL url, final Proxy proxy) {
		this(url);
		myProxy = proxy;
	}

	public void connect() throws ECommunicationException {
		try {
			mySession = new SessionXmlRpcTargetImpl(myUrl.toExternalForm(), CONNECTION_TIMEOUT);
		} catch (Throwable e) {
			throw new ECommunicationException(e);
		}
	}

	public void logon(final String username, final String password) throws ECommunicationException, EAuthorizationException {
		mySession.setCredentials(username, password);
		try {
			mySession.authenticate();
		} catch (AuthenticationFailedException e) {
			throw new EAuthorizationException(e);
		} catch (RemoteCallException e) {
			throw new ECommunicationException(e);
		}
	}
	
	private RemoteBuildServer getServerProxy() throws ECommunicationException {
		if(myServerProxy == null){
			myServerProxy = new RemoteBuildServerImpl(mySession, new ApplicationFacadeStub(), new VersionCheckerStub());
		}
		return myServerProxy;
	}
	
	public void logout() {
		mySession.logout();
	}
	
	public int getCurrentUser() {
		return mySession.getUserId();
	}
	
	@SuppressWarnings("unchecked")
	public synchronized Collection<ProjectData> getProjects() throws ECommunicationException {
		if(myProjects == null){
			final RemoteBuildServer serverProxy = getServerProxy();
			final Vector builds = serverProxy.getRegisteredProjects(false);
			myProjects = new ArrayList<ProjectData>(builds.size());
			for (Object typeData : builds) {
				final ProjectData projectData = (ProjectData)deserializeObject(typeData);
				myProjects.add(projectData);
			}
		}
		return myProjects;
	}
	
	public synchronized Collection<BuildTypeData> getConfigurations() throws ECommunicationException {
		final Collection<ProjectData> allProjects = getProjects();
		final ArrayList<BuildTypeData> configurations = new ArrayList<BuildTypeData>(allProjects.size() * 5);
		for(ProjectData project : allProjects){
			configurations.addAll(project.getBuildTypes());
		}
		return configurations;
	}
	
	public synchronized Collection<VcsRoot> getVcsRoots() throws ECommunicationException {
		final Collection<BuildTypeData> allConfigurations = getConfigurations();
		final HashMap<Long, VcsRoot> vcsRoots = new HashMap<Long, VcsRoot>();
		for(BuildTypeData configuration : allConfigurations){
			for(VcsRoot root : configuration.getVcsRoots()){
				if(!vcsRoots.containsKey(root.getId())){
					vcsRoots.put(root.getId(), root);
				}
			}
		}
		return vcsRoots.values();
	}
	
	public TeamServerSummaryData getSummary() throws ECommunicationException {
		final byte[] serializedStr = getServerProxy().getGZippedSummary(String.valueOf(getCurrentUser()), true);
		try {
			final TeamServerSummaryData data = unzipAndDeserializeObject(serializedStr);
			return data;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public synchronized VcsRoot getVcsRoot(long id) throws ECommunicationException {
		for(VcsRoot root : getVcsRoots()){
			if(root.getId() == id){
				return root;
			}
		}
		return null;
	}
	
	//TODO: move to utils
	private final static XStreamHolder ourXStreamHolder = new XStreamHolder() {
		protected void configureXStream(XStream xStream) {
			ServerXStreamFormat.formatXStream(xStream);
		}
	};
	
	//TODO: move to utils
	private static <T> T deserializeObject(final Object typeData) {
		return XStreamWrapper.<T>deserializeObject((String)typeData, ourXStreamHolder);
	}
	
	//TODO: move to utils
	private <T> T unzipAndDeserializeObject(final Object typeData) throws IOException {
		return XStreamWrapper.<T>unzipAndDeserializeObject((byte[])typeData, ourXStreamHolder);
	}
	

	static class VersionCheckerStub implements VersionChecker{

		public void checkServerVersion() throws IncompatiblePluginError {
			
		}
	}
	
	static class ApplicationFacadeStub implements ApplicationFacade {

		public Cancelable createCancelable() {
			return null;
		}

		public void onProcessCanceled() {
		}
		
	}

	public String getURL() {
		return mySession.getServerURL();
	}

	public AddToQueueResult addRemoteRunToQueue(ArrayList<AddToQueueRequest> batch, String myComments) throws ECommunicationException {
		return deserializeObject(getServerProxy().addToQueue(XStreamWrapper.serializeObjects(batch, ourXStreamHolder), myComments));
	}
	

}

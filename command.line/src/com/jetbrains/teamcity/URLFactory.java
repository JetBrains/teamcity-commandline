package com.jetbrains.teamcity;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import jetbrains.buildServer.util.FileUtil;

import org.apache.log4j.Logger;

import com.jetbrains.teamcity.resources.IShare;

public abstract class URLFactory {
	
	private static Logger LOGGER = Logger.getLogger(URLFactory.class) ;

	private URLFactory() {

	}

	public static URLFactory getFactory(IShare localRoot) {
		final String vcs = localRoot.getVcs();
		if (CVSUrlFactory.ID.equalsIgnoreCase(vcs)) {
			return new CVSUrlFactory(localRoot);
			
		} else if (SVNUrlFactory.ID.equalsIgnoreCase(vcs)) {
			return new SVNUrlFactory(localRoot);
			
		} else if (PerforceUrlFactory.ID.equalsIgnoreCase(vcs)) {
			return new PerforceUrlFactory(localRoot);
			
		} else if (TfsUrlFactory.ID.equalsIgnoreCase(vcs)) {
			return new TfsUrlFactory(localRoot);
			
		} else if (StarteamUrlFactory.ID.equalsIgnoreCase(vcs)) {
			return new StarteamUrlFactory(localRoot);
			
		}
		return null;
	}

	public abstract String getUrl(File file) throws IOException ;

	static class CVSUrlFactory extends URLFactory {
		
		static final String ID = "cvs"; //$NON-NLS-1$

		private String myCvsRoot;
		private String myModule;
		private File myLocalRoot;

//		private VcsRoot myRoot;

		public CVSUrlFactory(IShare localRoot) {
			myLocalRoot = new File(localRoot.getLocal());
			myCvsRoot = localRoot.getProperties().get("cvs-root"); //$NON-NLS-1$
			myModule = localRoot.getProperties().get("module-name"); //$NON-NLS-1$
//			cvs://:pserver:kdonskov@localhost:/cvs|tc-test/src/all/CVS.java
			//C:\work\tc-test\tc-test-CVS\src
		}

		@Override
		public String getUrl(File file) throws IOException {
			final String relativePath = Util.getRelativePath(myLocalRoot, file);
			final String url = MessageFormat.format("cvs://{0}|{1}/{2}", myCvsRoot, myModule, relativePath); //$NON-NLS-1$
			return url;
		}

	}

	
	static class SVNUrlFactory extends URLFactory {
		
		// svn://5c05b1c6-6b6d-794d-98af-4f7900fed0f9|trunk/tc-test-rusps-app-svn/src/all/New.java
		
		static final String ID = "svn"; //$NON-NLS-1$

		private File myLocalRoot;
		private String myRootId;

		public SVNUrlFactory(IShare localRoot) {
			myLocalRoot = new File(localRoot.getLocal());
			//ugly hack: get uuid from local 
			final File entries = new File(myLocalRoot.getAbsolutePath() + File.separator + ".svn" + File.separator + "entries"); //$NON-NLS-1$ //$NON-NLS-2$
			if(entries.exists()){
				try {
					final List<String> entriesContent = FileUtil.readFile(entries);
					if(entriesContent != null && entriesContent.size()>27){
						final String repoUUID = entriesContent.get(26);
						final String url = entriesContent.get(4);
						final String repoUrl = entriesContent.get(5);
						final String localRootSegment = url.substring(repoUrl.length() + 1, url.length());
						myRootId = MessageFormat.format("svn://{0}|{1}", repoUUID, localRootSegment); //$NON-NLS-1$
					}
				} catch (IOException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}

		@Override
		public String getUrl(File file) throws IOException {
			final String relativePath = Util.getRelativePath(myLocalRoot, file);
			final String url = MessageFormat.format("{0}/{1}", myRootId, relativePath); //$NON-NLS-1$
			return url;
		}

	}
	
	static class PerforceUrlFactory extends URLFactory {
		
		// perforce://1666:////depot/test-perforce-in-workspace/src/test_perforce_in_workspace/handlers/SampleHandler.java
		// perforce://localhost:1666:////depot/test-perforce-in-workspace/src/test_perforce_in_workspace/handlers/SampleHandler.java
		// perforce://rusps-app.SwiftTeams.local:1666:////depot/src/test_perforce_in/ActivatorRenamed.java
		// perforce://rusps-app:1666:////depot/src/other/ENotAdded.java
//		11	null	perforce	{port=rusps-app:1666, client-mapping=//depot/... //team-city-agent/..., p4-exe=p4, user=kdonskov, use-client=false}
		
		static final String ID = "perforce";//$NON-NLS-1$

		private File myLocalRoot;
		private String myPort;

		public PerforceUrlFactory(IShare localRoot) {
			myLocalRoot = new File(localRoot.getLocal());
			myPort = localRoot.getProperties().get("port"); //$NON-NLS-1$
		}

		@Override
		public String getUrl(File file) throws IOException {
			final String relativePath = Util.getRelativePath(myLocalRoot, file);
			final String url = MessageFormat.format("perforce://{0}:////depot/{1}", myPort, relativePath); //$NON-NLS-1$
			return url;
		}

	}
	
	static class TfsUrlFactory extends URLFactory {
		
		//http[s]://<server-path>:<server-port>/$foo/bar
		
		static final String ID = "tfs"; //$NON-NLS-1$
		
		private File myLocalRoot;
		
		private String myTfsRootId;

		public TfsUrlFactory(IShare localRoot) {
			myLocalRoot = new File(localRoot.getLocal());
			
			final String tfsRoot = localRoot.getProperties().get("tfs-root"); //$NON-NLS-1$
			final String tfsProtocol = localRoot.getProperties().get("tfs-protocol"); //$NON-NLS-1$
			final String tfsHost = localRoot.getProperties().get("tfs-server"); //$NON-NLS-1$
			final String tfsPort = localRoot.getProperties().get("tfs-port"); //$NON-NLS-1$
			
			myTfsRootId = MessageFormat.format("{0}://{1}://{2}:{3}/{4}", ID, tfsProtocol, tfsHost, tfsPort, tfsRoot); //$NON-NLS-1$
		}

		@Override
		public String getUrl(File file) throws IOException {
			final String relativePath = Util.getRelativePath(myLocalRoot, file);
			final String url = MessageFormat.format("{0}/{1}", myTfsRootId, relativePath); //$NON-NLS-1$
			return url;
		}

	}
	
	static class StarteamUrlFactory extends URLFactory {
//	    String result;
//	    String url = properties.get(StarteamProps.URL);
//	    String user = properties.get(StarteamProps.USERNAME);
//	    String pwd = properties.get(StarteamProps.PASSWORD);
//	    
//	    if(user != null && pwd != null) { // insert user/password into url
//	      StarTeamURL tmpUrl = new StarTeamURL(url);
//	      result = new StringBuilder(100).append("starteam://").
//	        append(user).append(":").append(pwd).append("@").append(tmpUrl.getHostName()).append(":").append(tmpUrl.getPort()).append("/")
//	        .append(tmpUrl.getProjectName()).append("/").append(tmpUrl.getPath()).toString();
//	    }
//	    else
//	      result = url;
//
//	    return result;
		
		
		static final String ID = "starteam"; //$NON-NLS-1$
		
		private File myLocalRoot;
		
		private String myTfsRootId;

		public StarteamUrlFactory(IShare localRoot) {
			myLocalRoot = new File(localRoot.getLocal());
			
			final String tfsRoot = localRoot.getProperties().get("tfs-root"); //$NON-NLS-1$
			final String tfsProtocol = localRoot.getProperties().get("tfs-protocol"); //$NON-NLS-1$
			final String tfsHost = localRoot.getProperties().get("tfs-server"); //$NON-NLS-1$
			final String tfsPort = localRoot.getProperties().get("tfs-port"); //$NON-NLS-1$
			
			myTfsRootId = MessageFormat.format("{0}://{1}://{2}:{3}/", ID, tfsProtocol, tfsHost, tfsPort, tfsRoot); //$NON-NLS-1$
			
		}

		@Override
		public String getUrl(File file) throws IOException {
			final String relativePath = Util.getRelativePath(myLocalRoot, file);
			final String url = MessageFormat.format("{0}/{1}", myTfsRootId, relativePath); //$NON-NLS-1$
			return url;
		}

	}
	
	

}

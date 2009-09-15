package com.jetbrains.teamcity;
public abstract class URLFactory {
	
}
//
//import java.io.File;
//import java.io.IOException;
//import java.text.MessageFormat;
//import java.util.List;
//import java.util.Map;
//
//import jetbrains.buildServer.util.FileUtil;
//import jetbrains.buildServer.vcs.VcsRoot;
//
//import org.apache.log4j.Logger;
//
//import com.jetbrains.teamcity.resources.IShare;
//import com.jetbrains.teamcity.resources.TCAccess;
//import com.jetbrains.teamcity.vcs.PerforceSupport;
//
//public abstract class URLFactory {
//	
//	private static final String[] EMPTY_MAPPING = new String[0];
//	
//	private static Logger LOGGER = Logger.getLogger(URLFactory.class) ;
//
//	private URLFactory() {
//
//	}
//	
//	public static URLFactory getFactory(final VcsRoot root) {
//		final String vcs = root.getVcsName();
//		
//		if (PerforceUrlFactory.ID.equalsIgnoreCase(vcs)) {
//			return new PerforceUrlFactory(root);
//			
////		} else if (SVNUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new SVNUrlFactory(root);
////			
////		} else if (CVSUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new CVSUrlFactory(root);
////			
////		} else if (TfsUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new TfsUrlFactory(root);
////			
////		} else if (StarteamUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new StarteamUrlFactory(root);
////
////		} else if (GitUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new GitUrlFactory(root);
////			
//		}
//		return null;
//	}
//
////	@Deprecated
////	public static URLFactory getFactory(IShare localRoot) {
////		final String vcs = localRoot.getVcs();
////		if (CVSUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new CVSUrlFactory(localRoot);
////			
////		} else if (SVNUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new SVNUrlFactory(localRoot);
////			
////		} else if (PerforceUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new PerforceUrlFactory(localRoot);
////			
////		} else if (TfsUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new TfsUrlFactory(localRoot);
////			
////		} else if (StarteamUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new StarteamUrlFactory(localRoot);
////
////		} else if (GitUrlFactory.ID.equalsIgnoreCase(vcs)) {
////			return new GitUrlFactory(localRoot);
////			
////		}
////		return null;
////	}
////	
////	@Deprecated
////	public abstract String getUrl(File file) throws IOException, ECommunicationException;
//	
//	public abstract String[] getRoots();
//	
//	public abstract String getRootLocation(final String vcsRoot, final String localPostfix);
//	
//
//	static class CVSUrlFactory extends URLFactory {
//
//		static final String ID = "cvs"; //$NON-NLS-1$
//
//		private String myCvsRoot;
//		private String myModule;
//		private File myLocalRoot;
//
//		public CVSUrlFactory(IShare localRoot) {
//			myLocalRoot = new File(localRoot.getLocal());
//			myCvsRoot = localRoot.getProperties().get("cvs-root"); //$NON-NLS-1$
//			myModule = localRoot.getProperties().get("module-name"); //$NON-NLS-1$
////			cvs://:pserver:kdonskov@localhost:/cvs|tc-test/src/all/CVS.java
//			//C:\work\tc-test\tc-test-CVS\src
//		}
//
////		@Override
////		public String getUrl(File file) throws IOException {
////			final String relativePath = Util.getRelativePath(myLocalRoot, file);
////			final String url = MessageFormat.format("cvs://{0}|{1}/{2}", myCvsRoot, myModule, relativePath); //$NON-NLS-1$
////			return url;
////		}
//
//		@Override
//		public String[] getRoots() {
//			//TODO: implements multiple modules
//			return new String[] { MessageFormat.format("cvs://{0}|{1}{2}",
//					myCvsRoot, myModule) };
//		}
//		
//		@Override
//		public String getRootLocation(final String vcsRoot, final String localPostfix){
//			return null;
//		}
//
//	}
//
//	
//	static class SVNUrlFactory extends URLFactory {
//		
//		// svn://5c05b1c6-6b6d-794d-98af-4f7900fed0f9|trunk/tc-test-rusps-app-svn/src/all/New.java
//		
//		static final String SVN_FOLDER = ".svn";
//		static final String ENTRIES_FILE = "entries";
//
//		static final String ID = "svn"; //$NON-NLS-1$
//
//		private File myLocalRoot;
//		private String myRootId;
//
//		SVNUrlFactory(IShare localRoot) {
//			myLocalRoot = new File(localRoot.getLocal());
//			//FIXME: ugly hack: get uuid from local 
//			final String rootPath = myLocalRoot.getAbsolutePath();
//			final File entries = getEntriesFile(rootPath); //$NON-NLS-1$ //$NON-NLS-2$
//			if(entries.exists()){
//				try {
//					final List<String> entriesContent = FileUtil.readFile(entries);
//					if(entriesContent != null && entriesContent.size()>27){
//						final String repoUUID = entriesContent.get(26);
//						final String url = entriesContent.get(4);
//						final String repoUrl = entriesContent.get(5);
//						if (url.length() > repoUrl.length()) {
//							final String localRootSegment = url.substring(repoUrl.length() + 1, url.length());
//							myRootId = MessageFormat.format("svn://{0}|{1}/", repoUUID, localRootSegment); //$NON-NLS-1$
//						}  else {
//							myRootId = MessageFormat.format("svn://{0}|/", repoUUID); //$NON-NLS-1$
//						}
//					}
//				} catch (IOException e) {
//					LOGGER.error(e.getMessage(), e);
//				}
//			}
//		}
//
//		static File getEntriesFile(String path) {
//			if(path == null){
//				return null;
//			}
//			final File entries = new File(path + File.separator + SVN_FOLDER + File.separator + ENTRIES_FILE);
//			if(entries.exists()){
//				return entries;
//			}
//			//see up to hierarchy to find in parent folder
//			return getEntriesFile(new File(path).getParent());
//		}
//
////		@Override
////		public String getUrl(File file) throws IOException {
////			final String relativePath = Util.getRelativePath(myLocalRoot, file);
////			final String url = MessageFormat.format("{0}{1}", myRootId, relativePath); //$NON-NLS-1$
////			return url;
////		}
//		
//		@Override
//		public String[] getRoots() {
//			//SVN does not support mappings
//			return EMPTY_MAPPING;
//		}
//		
//		@Override
//		public String getRootLocation(final String vcsRoot, final String localPostfix){
//			return null;
//		}
//		
//		
//
//	}
//	
//	static class PerforceUrlFactory extends URLFactory {
//		
//		private static final String ID = "perforce"; //$NON-NLS-1$
//		
//		private static final String PORT = "port"; //$NON-NLS-1$
//
//		private File myLocalRoot;
//		private String myTeamCityPort;
//		private String myTeamCityMapping;//"//depot";
//		/**
//		 * overriding TeamCity's Server properties
//		 */
//		private String myP4Port;
//		private String myP4Client;
//		private String myP4User;
//		private String myP4Password;
//
////		private IShare myShare;
//
//		private VcsRoot myVcsRoot;
//
////		public PerforceUrlFactory(IShare localRoot) {
////			
////			myShare = localRoot;
////			
////			setupOverridingProperties();
////			
////			myLocalRoot = new File(localRoot.getLocal());
////			//check the property was set and override TC values if so.
////			if(myP4Port != null){
////				myTeamCityPort = myP4Port; //$NON-NLS-1$
////				
////			} else {
////				myTeamCityPort = localRoot.getProperties().get(PORT); //$NON-NLS-1$
////			}
////			LOGGER.debug(MessageFormat.format("$P4PORT set to {0}", myTeamCityPort)); //$NON-NLS-1$
////			//check Default mapping and use it if exists
////			final String defaultMapping = TCAccess.getInstance().getDefaultMapping(localRoot);
////			//nothing set. use cached root's property
////			final String uniqueRoot = PerforceSupport.findPerforceRoot(localRoot.getProperties(), defaultMapping);
////			if(uniqueRoot != null){
////				myTeamCityMapping = uniqueRoot;
////				LOGGER.debug(MessageFormat.format("Root mapping set to {0}", myTeamCityMapping)); //$NON-NLS-1$
////			}
////		}
//		
//		public PerforceUrlFactory(final VcsRoot root) {
//			myVcsRoot = root;
//			myTeamCityPort = myVcsRoot.getProperties().get(PORT);
//		}
//
//		@Override
//		public String[] getRoots() {
//			final Map<String, String> properties = myVcsRoot.getProperties();
//			final String clientMapping = properties.get(PerforceSupport.CLIENT_MAPPING);
//			if(clientMapping != null && PerforceSupport.findPerforceRoot(properties, null) == null){
//				return clientMapping.split("[\n\r]");
//			}
//			return EMPTY_MAPPING;
//		}
//		
//		public String getRootLocation(final String subroot, final String localPostfix) {
////			perforce://rusps-app01:1666:////depot/src/
//			final String repoPath = PerforceSupport.findPerforceRoot(subroot);
//			return MessageFormat.format("perforce://{0}://{1}", myTeamCityPort, repoPath);
//		}
//		
////		private void setupOverridingProperties() {
////			myP4Port = System.getProperty(Constants.PERFORCE_P4PORT);
////			myP4Client = System.getProperty(Constants.PERFORCE_P4CLIENT);
////			myP4User = System.getProperty(Constants.PERFORCE_P4USER);
////			myP4Password = System.getProperty(Constants.PERFORCE_P4PASSWORD);
////		}
////
////		@Override
////		public String getUrl(File file) throws IOException, ECommunicationException {
////			if(myTeamCityMapping != null){
////				final String relativePath = Util.getRelativePath(myLocalRoot, file);
////				final String url = MessageFormat.format("perforce://{0}://{1}/{2}", myTeamCityPort, myTeamCityMapping, relativePath); //$NON-NLS-1$
////				return url;
////			} else {
////				final String path = PerforceSupport.getRepositoryPath(myTeamCityPort, myP4Client, myP4User, myP4Password, file);
////				if(path != null){
////					final String url = MessageFormat.format("perforce://{0}://{1}", myTeamCityPort, path); //$NON-NLS-1$
////					return url;
////				}
////			}
////			return null;
////		}
//
//	}
//	
//	static class TfsUrlFactory extends URLFactory {
//		
//		//http[s]://<server-path>:<server-port>/$foo/bar
//		
//		static final String ID = "tfs"; //$NON-NLS-1$
//		
//		private File myLocalRoot;
//		
//		private String myTfsRootId;
//
//		public TfsUrlFactory(IShare localRoot) {
//			myLocalRoot = new File(localRoot.getLocal());
//			
//			final String tfsRoot = localRoot.getProperties().get("tfs-root"); //$NON-NLS-1$
//			final String tfsProtocol = localRoot.getProperties().get("tfs-protocol"); //$NON-NLS-1$
//			final String tfsHost = localRoot.getProperties().get("tfs-server"); //$NON-NLS-1$
//			final String tfsPort = localRoot.getProperties().get("tfs-port"); //$NON-NLS-1$
//			
//			myTfsRootId = MessageFormat.format("{0}://{1}://{2}:{3}/{4}", ID, tfsProtocol, tfsHost, tfsPort, tfsRoot); //$NON-NLS-1$
//		}
//
////		@Override
////		public String getUrl(File file) throws IOException {
////			final String relativePath = Util.getRelativePath(myLocalRoot, file);
////			final String url = MessageFormat.format("{0}/{1}", myTfsRootId, relativePath); //$NON-NLS-1$
////			return url;
////		}
//
//		@Override
//		public String[] getRoots() {
//			return EMPTY_MAPPING;
//		}
//		
//		@Override
//		public String getRootLocation(final String vcsRoot, final String localPostfix){
//			return null;
//		}
//		
//
//	}
//	
////	static class StarteamUrlFactory extends URLFactory {
//////	    String result;
//////	    String url = properties.get(StarteamProps.URL);
//////	    String user = properties.get(StarteamProps.USERNAME);
//////	    String pwd = properties.get(StarteamProps.PASSWORD);
//////	    
//////	    if(user != null && pwd != null) { // insert user/password into url
//////	      StarTeamURL tmpUrl = new StarTeamURL(url);
//////	      result = new StringBuilder(100).append("starteam://").
//////	        append(user).append(":").append(pwd).append("@").append(tmpUrl.getHostName()).append(":").append(tmpUrl.getPort()).append("/")
//////	        .append(tmpUrl.getProjectName()).append("/").append(tmpUrl.getPath()).toString();
//////	    }
//////	    else
//////	      result = url;
//////
//////	    return result;
////		
////		
////		static final String ID = "starteam"; //$NON-NLS-1$
////		
////		private File myLocalRoot;
////		
////		private String myTfsRootId;
////
////		public StarteamUrlFactory(IShare localRoot) {
////			myLocalRoot = new File(localRoot.getLocal());
////			
////			final String tfsRoot = localRoot.getProperties().get("tfs-root"); //$NON-NLS-1$
////			final String tfsProtocol = localRoot.getProperties().get("tfs-protocol"); //$NON-NLS-1$
////			final String tfsHost = localRoot.getProperties().get("tfs-server"); //$NON-NLS-1$
////			final String tfsPort = localRoot.getProperties().get("tfs-port"); //$NON-NLS-1$
////			
////			myTfsRootId = MessageFormat.format("{0}://{1}://{2}:{3}/", ID, tfsProtocol, tfsHost, tfsPort, tfsRoot); //$NON-NLS-1$
////			
////		}
////
//////		@Override
//////		public String getUrl(File file) throws IOException {
//////			final String relativePath = Util.getRelativePath(myLocalRoot, file);
//////			final String url = MessageFormat.format("{0}/{1}", myTfsRootId, relativePath); //$NON-NLS-1$
//////			return url;
//////		}
////
////		@Override
////		public String[] getRoots() {
////			return EMPTY_MAPPING;
////		}
////		
////	}
//	
////	static class GitUrlFactory extends URLFactory {
////		
////		//git://rusps-app01/repo/jgit#master
////		
////		static final String ID = "jetbrains.git"; //$NON-NLS-1$
////		
////		private File myLocalRoot;
////		
////		private String myTfsRootId;
////
////		public GitUrlFactory(IShare localRoot) {
////			myLocalRoot = new File(localRoot.getLocal());
////			
////			final String branch = localRoot.getProperties().get("branch"); //$NON-NLS-1$
////			final String url = localRoot.getProperties().get("url"); //$NON-NLS-1$
////			
////			myTfsRootId = MessageFormat.format("{0}://{1}#{2}", ID, url, branch); //$NON-NLS-1$
////		}
////
//////		@Override
//////		public String getUrl(File file) throws IOException {
//////			final String relativePath = Util.getRelativePath(myLocalRoot, file);
//////			final String url = MessageFormat.format("{0}|{1}", myTfsRootId, relativePath); //$NON-NLS-1$
//////			return url;
//////		}
////		
////		@Override
////		public String[] getRoots() {
////			return EMPTY_MAPPING;
////		}
////		
////
////	}
//	
//
//}

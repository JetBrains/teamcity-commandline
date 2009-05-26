package com.jetbrains.teamcity;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import jetbrains.buildServer.util.FileUtil;

import com.jetbrains.teamcity.resources.IVCSRoot;

public abstract class URLFactory {

	private URLFactory() {

	}

	public static URLFactory getFactory(IVCSRoot localRoot) {
		final String vcs = localRoot.getVcs();
		if ("cvs".equalsIgnoreCase(vcs)) {
			return new CVSUrlFactory(localRoot);
		} else if ("svn".equalsIgnoreCase(vcs)) {
			return new SVNUrlFactory(localRoot);
		} else if ("perforce".equalsIgnoreCase(vcs)) {
			return new PerforceUrlFactory(localRoot);
		}
		return null;
	}

	public abstract String getUrl(File file) throws IOException ;

	static class CVSUrlFactory extends URLFactory {

		private String myCvsRoot;
		private String myModule;
		private File myLocalRoot;

//		private VcsRoot myRoot;

		public CVSUrlFactory(IVCSRoot localRoot) {
			myLocalRoot = new File(localRoot.getLocal());
			myCvsRoot = localRoot.getProperties().get("cvs-root");
			myModule = localRoot.getProperties().get("module-name");
//			cvs://:pserver:kdonskov@localhost:/cvs|tc-test/src/all/CVS.java
			//C:\work\tc-test\tc-test-CVS\src
		}

		@Override
		public String getUrl(File file) throws IOException {
			final String relativePath = Util.getRelativePath(myLocalRoot, file);
			final String url = MessageFormat.format("cvs://{0}|{1}/{2}", myCvsRoot, myModule, relativePath);
			return url;
		}

	}

	static class SVNUrlFactory extends URLFactory {
		
		// svn://5c05b1c6-6b6d-794d-98af-4f7900fed0f9|trunk/tc-test-rusps-app-svn/src/all/New.java

		private File myLocalRoot;
		private String myRootId;

		public SVNUrlFactory(IVCSRoot localRoot) {
			myLocalRoot = new File(localRoot.getLocal());
			//ugly hack: get uuid from local 
			final File entries = new File(myLocalRoot.getAbsolutePath() + File.separator + ".svn" + File.separator + "entries");
			if(entries.exists()){
				try {
					final List<String> entriesContent = FileUtil.readFile(entries);
					if(entriesContent != null && entriesContent.size()>27){
						final String repoUUID = entriesContent.get(26);
						final String url = entriesContent.get(4);
						final String repoUrl = entriesContent.get(5);
						final String localRootSegment = url.substring(repoUrl.length() + 1, url.length());
						myRootId = MessageFormat.format("svn://{0}|{1}", repoUUID, localRootSegment);
					}
				} catch (IOException e) {
					Logger.log(SVNUrlFactory.class.getName(), e);
				}
			}
		}

		@Override
		public String getUrl(File file) throws IOException {
			final String relativePath = Util.getRelativePath(myLocalRoot, file);
			final String url = MessageFormat.format("{0}/{1}", myRootId, relativePath);
			return url;
		}

	}
	
	static class PerforceUrlFactory extends URLFactory {
		
		// perforce://1666:////depot/test-perforce-in-workspace/src/test_perforce_in_workspace/handlers/SampleHandler.java
		// perforce://localhost:1666:////depot/test-perforce-in-workspace/src/test_perforce_in_workspace/handlers/SampleHandler.java
		// perforce://rusps-app.SwiftTeams.local:1666:////depot/src/test_perforce_in/ActivatorRenamed.java
		// perforce://rusps-app:1666:////depot/src/other/ENotAdded.java
//		11	null	perforce	{port=rusps-app:1666, client-mapping=//depot/... //team-city-agent/..., p4-exe=p4, user=kdonskov, use-client=false}
		

		private File myLocalRoot;
		private String myPort;

		public PerforceUrlFactory(IVCSRoot localRoot) {
			myLocalRoot = new File(localRoot.getLocal());
			myPort = localRoot.getProperties().get("port");
		}

		@Override
		public String getUrl(File file) throws IOException {
			final String relativePath = Util.getRelativePath(myLocalRoot, file);
			final String url = MessageFormat.format("perforce://{0}:////depot/{1}", myPort, relativePath);
			return url;
		}

	}
	

}

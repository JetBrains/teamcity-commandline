package com.jetbrains.teamcity;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import jetbrains.buildServer.vcs.VcsRoot;

import com.jetbrains.teamcity.resources.IVCSRoot;

public abstract class URLFactory {

	private URLFactory() {

	}

	public static URLFactory getFactory(IVCSRoot localRoot, final VcsRoot remoteRoot) {
		final String vcs = remoteRoot.getVcsName();
		if ("cvs".equalsIgnoreCase(vcs)) {
			return new CVSUrlFactory(localRoot, remoteRoot);
		} else if ("svn".equalsIgnoreCase(vcs)) {
			return new SVNUrlFactory(remoteRoot);
		} else if ("perforce".equalsIgnoreCase(vcs)) {
			return new PerforceUrlFactory(localRoot, remoteRoot);
		}
		return null;
	}

	public abstract String getUrl(File file) throws IOException ;

	static class CVSUrlFactory extends URLFactory {

		private String myCvsRoot;
		private String myModule;
		private File myLocalRoot;

//		private VcsRoot myRoot;

		public CVSUrlFactory(IVCSRoot localRoot, VcsRoot remoteRoot) {
			myLocalRoot = new File(localRoot.getLocal());
			myCvsRoot = remoteRoot.getProperties().get("cvs-root");
			myModule = remoteRoot.getProperties().get("module-name");
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

		private VcsRoot myRoot;

		public SVNUrlFactory(VcsRoot root) {
			myRoot = root;
		}

		@Override
		public String getUrl(File file) {
			return null;
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

		public PerforceUrlFactory(IVCSRoot localRoot, VcsRoot root) {
			myLocalRoot = new File(localRoot.getLocal());
			myPort = root.getProperties().get("port");
		}

		@Override
		public String getUrl(File file) throws IOException {
			final String relativePath = Util.getRelativePath(myLocalRoot, file);
			final String url = MessageFormat.format("perforce://{0}:////depot/{1}", myPort, relativePath);
			return url;
		}

	}
	

}

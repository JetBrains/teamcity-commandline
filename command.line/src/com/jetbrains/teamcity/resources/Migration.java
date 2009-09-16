package com.jetbrains.teamcity.resources;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.util.FileUtil;

import org.apache.log4j.Logger;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.command.Args;
import com.jetbrains.teamcity.command.ICommand;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

/**
 * Scans for all existing IShares and tries to convert them into new format 
 *
 */
@Deprecated
public class Migration implements ICommand {
	
	public static void main(String[]  args) throws Exception {
		final Migration command = new Migration();
		command.execute(null, null, null);
		System.err.println(command.getResultDescription());
	}

	public static final String ID = "migrate";

	private String myResultDescription;

	public void validate(Args args) throws IllegalArgumentException {
	}

	public void execute(final Server server, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError {
		final ArrayList<File> migrated = new ArrayList<File>();
		final ArrayList<File> skipped = new ArrayList<File>();
		final Collection<IShare> shares = TCAccess.getInstance().roots();
		for(final IShare share:  shares){
			final URLFactory factory = URLFactory.getFactory(share);
			final File shareRoot = new File(share.getLocal()).getAbsoluteFile();
			if(factory != null){
				final String vcsRoot = factory.getRootLocation();
				final TCWorkspace workspace = new TCWorkspace(shareRoot);
				workspace.setSharing(Collections.<File, String>singletonMap(shareRoot, vcsRoot));
				migrated.add(shareRoot);
			} else {
				skipped.add(shareRoot);
			}
		}
		myResultDescription = MessageFormat.format("Shares for \"{0}\" migrated successfully.\nHas not been migrated: \"{1}\"", migrated, skipped);
	}

	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired(final Args args) {
		return false;
	}

	public String getUsageDescription() {
		return MessageFormat.format("{0}\nusage: {1}", getCommandDescription(),
				getId());
	}

	public String getCommandDescription() {
		return "Converts old Shares information into new format";
	}

	public String getResultDescription() {
		return myResultDescription;
	}

	@Deprecated
	static abstract class URLFactory {

		private static Logger LOGGER = Logger.getLogger(URLFactory.class);

		private URLFactory() {

		}

		public static URLFactory getFactory(final IShare share) {
			final String vcs = share.getVcs();

			if (PerforceUrlFactory.ID.equalsIgnoreCase(vcs)) {
				return new PerforceUrlFactory(share);

			} else if (SVNUrlFactory.ID.equalsIgnoreCase(vcs)) {
				return new SVNUrlFactory(share);

			} else if (CVSUrlFactory.ID.equalsIgnoreCase(vcs)) {
				return new CVSUrlFactory(share);
			}
			return null;
		}

		public abstract String getRootLocation();

		static class CVSUrlFactory extends URLFactory {

			static final String ID = "cvs"; //$NON-NLS-1$

			private String myRootId;

			public CVSUrlFactory(IShare localRoot) {
				final String cvsRoot = localRoot.getProperties().get("cvs-root"); //$NON-NLS-1$
				final String module = localRoot.getProperties().get("module-name"); //$NON-NLS-1$
				myRootId = MessageFormat.format("cvs://{0}|{1}{2}", cvsRoot, module);
				// cvs://:pserver:kdonskov@localhost:/cvs|tc-test/src/all/CVS.java
				// C:\work\tc-test\tc-test-CVS\src
			}

			@Override
			public String getRootLocation() {
				return myRootId;
			}

		}

		static class SVNUrlFactory extends URLFactory {

			// svn://5c05b1c6-6b6d-794d-98af-4f7900fed0f9|trunk/tc-test-rusps-app-svn/src/all/New.java

			static final String SVN_FOLDER = ".svn";
			static final String ENTRIES_FILE = "entries";

			static final String ID = "svn"; //$NON-NLS-1$

			private File myLocalRoot;
			private String myRootId;

			SVNUrlFactory(IShare localRoot) {
				myLocalRoot = new File(localRoot.getLocal());
				// FIXME: ugly hack: get uuid from local
				final String rootPath = myLocalRoot.getAbsolutePath();
				final File entries = getEntriesFile(rootPath); //$NON-NLS-1$ //$NON-NLS-2$
				if (entries.exists()) {
					try {
						final List<String> entriesContent = FileUtil
								.readFile(entries);
						if (entriesContent != null
								&& entriesContent.size() > 27) {
							final String repoUUID = entriesContent.get(26);
							final String url = entriesContent.get(4);
							final String repoUrl = entriesContent.get(5);
							if (url.length() > repoUrl.length()) {
								final String localRootSegment = url.substring(
										repoUrl.length() + 1, url.length());
								myRootId = MessageFormat
										.format(
												"svn://{0}|{1}/", repoUUID, localRootSegment); //$NON-NLS-1$
							} else {
								myRootId = MessageFormat.format(
										"svn://{0}|/", repoUUID); //$NON-NLS-1$
							}
						}
					} catch (IOException e) {
						LOGGER.error(e.getMessage(), e);
					}
				}
			}

			static File getEntriesFile(String path) {
				if (path == null) {
					return null;
				}
				final File entries = new File(path + File.separator
						+ SVN_FOLDER + File.separator + ENTRIES_FILE);
				if (entries.exists()) {
					return entries;
				}
				// see up to hierarchy to find in parent folder
				return getEntriesFile(new File(path).getParent());
			}

			@Override
			public String getRootLocation() {
				return myRootId;
			}

		}

		static class PerforceUrlFactory extends URLFactory {

			private static final String ID = "perforce"; //$NON-NLS-1$

			private static final String PORT = "port"; //$NON-NLS-1$

			private String myRootId;

			public PerforceUrlFactory(final IShare share) {
				myRootId = MessageFormat.format("perforce://{0}://{1}", 
						share.getProperties().get(PORT),
						PerforceSupport.findPerforceRoot(share.getProperties().get(PerforceSupport.CLIENT_MAPPING)));
			}

			public String getRootLocation() {
				return myRootId;
			}

		}

	}
	
	static class PerforceSupport {
		
		private static Logger LOGGER = Logger.getLogger(PerforceSupport.class) ;
		
		private static final String NEWLINE_PATTERN = "[\n\r]";
		private static final String SPACE = "\\s+"; //$NON-NLS-1$
		private static final String SLASH = "/"; //$NON-NLS-1$
		
		static final String TEAM_CITY_AGENT = "//team-city-agent/"; //$NON-NLS-1$
		static final String TEAM_CITY_AGENT_ROOT = "//team-city-agent/..."; //$NON-NLS-1$
		
		public static final String USE_CLIENT = "use-client"; //$NON-NLS-1$
		public static final String CLIENT_MAPPING = "client-mapping"; //$NON-NLS-1$

		public static String findPerforceRoot(final Map<String, String> properties, final String defaultMapping) {
			LOGGER.debug(MessageFormat.format("Seeking for Perforce root in {0}, DefaultMapping: {1}", properties, defaultMapping)); //$NON-NLS-1$
			if(defaultMapping == null){
				final String useClient = properties.get(USE_CLIENT);
				if(!Boolean.TRUE.equals(Boolean.parseBoolean(useClient))){
					final String clientMapping = properties.get(CLIENT_MAPPING); //$NON-NLS-1$
					if(clientMapping != null && clientMapping.trim().length() != 0){
						final String uniqueRoot = findPerforceRoot(clientMapping);
						if(uniqueRoot != null){
							LOGGER.debug(MessageFormat.format("Unique root found: {0}", uniqueRoot)); //$NON-NLS-1$					
							return uniqueRoot;
						}
					}
				}
			} else {
				//parse default and convert into prefix
				final String[] columns = defaultMapping.trim().split(SPACE);
				if(columns.length>0){
					final String root = columns[0].trim();
					return root.substring(0, root.lastIndexOf(SLASH));
				}
			}
			LOGGER.debug("No Unique root found"); //$NON-NLS-1$
			return null;
		}
		
		/**
		 * looking for unique string line contains "//team-city-agent/" token
		 */
		public static String findPerforceRoot(final String clientMapping) {
			try{
				final String[] lines = clientMapping.split(NEWLINE_PATTERN);
				if(lines.length == 1){
					final String[] columns = lines[0].split(SPACE);
					final String root = columns[0].trim();
					return root.substring(0, root.lastIndexOf(SLASH));
				} else {
					String root = null;
					for(String line : lines){
						final String[] columns = line.split(SPACE);
						if (columns.length > 1) {
							if(columns[1].trim().equalsIgnoreCase(TEAM_CITY_AGENT_ROOT)){
								return columns[0].trim().substring(0, columns[0].lastIndexOf(SLASH));
							} else if (columns[1].toLowerCase().startsWith(TEAM_CITY_AGENT)){
								if(root == null){
									root = columns[0].trim();
								} else {
									return null; //there are any entries for mapping 
								}
							}
						}
					}
					if(root!=null){
						return root.substring(0, root.lastIndexOf(SLASH));
					}
				}
			} catch (Exception e){
				LOGGER.error(MessageFormat.format("Unexpected error over \"{0}\" ClientMapping parsing", clientMapping), e); //$NON-NLS-1$
			}
			return null;
		}

	}


}

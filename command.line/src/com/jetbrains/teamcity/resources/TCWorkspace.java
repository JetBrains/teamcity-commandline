package com.jetbrains.teamcity.resources;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.jetbrains.teamcity.Storage;

public class TCWorkspace {
	
	private static Logger LOGGER = Logger.getLogger(TCWorkspace.class) ;
	
	public static final String TCC_ADMIN_FILE = Messages.getString("TCWorkspace.per.folder.admin.file"); //$NON-NLS-1$
	
	public static final String TCC_GLOBAL_ADMIN_FILE = System
			.getProperty("user.home") //$NON-NLS-1$
			+ File.separator
			+ Storage.TC_CLI_HOME
			+ File.separator
			+ Messages.getString("TCWorkspace.global.admin.file"); //$NON-NLS-1$
	
	private HashMap<File, ITCResourceMatcher> myCache = new HashMap<File, ITCResourceMatcher>();

	private File myRootFolder;

	private ITCResourceMatcher myGlobalMatcher;
	
	private ITCResourceMatcher myOverridingMatcher;
	
	public TCWorkspace(final File rootFolder){
		if(rootFolder == null){
			throw new IllegalArgumentException("Root directory cannot be null");
		}
		try {
			myRootFolder = rootFolder.getAbsoluteFile().getCanonicalFile();
			//setup global admin
			final File defaultConfig = getGlobalAdminFile();
			if(defaultConfig != null && defaultConfig.exists()){
				myGlobalMatcher = new FileBasedMatcher(defaultConfig);
			} else {
				LOGGER.debug(MessageFormat.format("Default Admin file \"{0}\" is not found", defaultConfig));			
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected File getGlobalAdminFile() {
		final String globalConfigEnv = System.getenv("TC_DEFAULT_CONFIG");
		if(globalConfigEnv != null && globalConfigEnv.length()>0){
			LOGGER.debug(String.format("Default Admin file \"%s\" got from Environment variable", globalConfigEnv));
			return new File(globalConfigEnv);
		}
		final String myDefaultConfigFile = TCC_GLOBAL_ADMIN_FILE;
		LOGGER.debug(String.format("Default Admin file \"%s\" got from default location", myDefaultConfigFile));
		return new File(myDefaultConfigFile);
	}
	
	public TCWorkspace(final File rootFolder, final ITCResourceMatcher externMatcher){
		this(rootFolder);
		myOverridingMatcher = externMatcher;
		LOGGER.debug(String.format("Overriding Matcher set to \"%s\"", externMatcher)); //$NON-NLS-1$
	}
	
	public File getRoot() {
		return myRootFolder;
	}

	static ITCResourceMatcher getMatcherFor(final File local) throws IllegalArgumentException {
		if(local == null){
			throw new IllegalArgumentException("File cannot be null");
		}
		//per-folder search
		try {
			File folder = local.getParentFile();
			if(folder != null){
				folder = folder.getCanonicalFile().getAbsoluteFile();
				final File adminFile = new File(folder, TCC_ADMIN_FILE);
				if(adminFile != null && adminFile.exists()){
					return new FileBasedMatcher(adminFile);
				} else {
					return getMatcherFor(folder);
				}
			}
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return null;
	}
	
	public ITCResource getTCResource(File local) throws IllegalArgumentException {
		if(local == null){
			throw new IllegalArgumentException("File cannot be null");
		}
		try {
			local = local.getAbsoluteFile().getCanonicalFile();
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		ITCResourceMatcher matcher;
		//set to OverridingMatcher if defined
		if(myOverridingMatcher != null){
			matcher = myOverridingMatcher;
		} else {
			//look into cache(cache keep file for files resides in same folder only)
			matcher = myCache.get(local.getParentFile()); 
			if(matcher == null){
				//seek for uncached
				matcher = getMatcherFor(local);
				if(matcher == null){
					//look into Global
					matcher = myGlobalMatcher;
					if(myGlobalMatcher == null){
						LOGGER.debug(MessageFormat.format("Neither Local nor Global admin files found for \"{0}\"", local));
						return null;
					}
				}
			}
		}
		if(matcher != null){
			//cache found
			myCache.put(local.getParentFile(), matcher);
			final ITCResourceMatcher.Matching matching = matcher.getMatching(local);
			if(matching == null){
				LOGGER.debug(MessageFormat.format("No Matching found for \"{0}\"", local));
				return null;
			}
			//All found
			final String prefix = matching.getTCID();
			final String relativePath = matching.getRelativePath();
			return new TCResource(local, MessageFormat.format("{0}/{1}", prefix, relativePath));  //$NON-NLS-1$
		}
		return null;
	}
	
	static class TCResource implements ITCResource {
		
		private File myLocal;
		private String myRepositoryPath;

		TCResource(File local, String repositoryPath){
			myLocal = local;
			myRepositoryPath = repositoryPath;
		}

		public File getLocal() {
			return myLocal;
		}

		public String getRepositoryPath() {
			return myRepositoryPath;
		}
		
		@Override
		public String toString() {
			return MessageFormat.format("local={0}, repo={1}", getLocal(), getRepositoryPath()); //$NON-NLS-1$
		}
		
	}

}

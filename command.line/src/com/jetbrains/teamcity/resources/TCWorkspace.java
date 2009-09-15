package com.jetbrains.teamcity.resources;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.jetbrains.teamcity.Storage;

public class TCWorkspace {
	
	private static Logger LOGGER = Logger.getLogger(TCWorkspace.class) ;
	
	public static final String TCC_ADMIN_FILE = ".tcc"; //$NON-NLS-1$
	
	private HashMap<File, ITCResourceMatcher> myCache = new HashMap<File, ITCResourceMatcher>();

	private File myRootFolder;

	private ITCResourceMatcher myGlobalMatcher;
	
	public TCWorkspace(final File rootFolder){
		if(rootFolder == null){
			throw new IllegalArgumentException("Root directory cannot be null");
		}
		myRootFolder = rootFolder;
		//setup global admin
		final String home = System.getProperty("user.home"); //$NON-NLS-1$
		final String myDefaultConfigFile = home + File.separator + Storage.TC_CLI_HOME + File.separator + ".tcc.global.config";
		final File defaultConfig = new File(myDefaultConfigFile);
		if(defaultConfig.exists()){
			myGlobalMatcher = new FileBasedMatcher(defaultConfig);
		} else {
			LOGGER.debug(MessageFormat.format("Default Admin file \"{0}\" is not found", myDefaultConfigFile));			
		}
	}
	
	public TCWorkspace(final File rootFolder, final ITCResourceMatcher externMatcher){
		this(rootFolder);
		if(externMatcher != null){
			myGlobalMatcher = externMatcher;
		} else {
			LOGGER.debug(MessageFormat.format("Extern matcher is null", externMatcher));			
		}
	}
	
	public File getRoot() {
		return myRootFolder;
	}

	static ITCResourceMatcher getMatcherFor(final File local) throws IllegalArgumentException {
		if(local == null){
			throw new IllegalArgumentException("File cannot be null");
		}
		//per-folder search
		final File folder = local.getParentFile();
		if(folder != null){
			try {
				final File adminFile = new File(folder.getCanonicalFile().getAbsoluteFile(), TCC_ADMIN_FILE);
				if(adminFile != null && adminFile.exists()){
					return new FileBasedMatcher(adminFile);
				} else {
					return getMatcherFor(folder);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return null;
	}
	
	public ITCResource getTCResource(final File local) throws IllegalArgumentException {
		if(local == null){
			throw new IllegalArgumentException("File cannot be null");
		}
		//look into cache(cache keep file for files resides in same folder only)
		ITCResourceMatcher matcher = myCache.get(local.getParentFile()); 
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
			return new TCResource(local, MessageFormat.format("{0}/{1}", prefix, relativePath)); 
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
			return MessageFormat.format("local={0}, repo={1}", getLocal(), getRepositoryPath());
		}
		
	}

}

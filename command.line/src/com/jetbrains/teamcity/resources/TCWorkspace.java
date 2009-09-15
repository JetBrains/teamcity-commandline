package com.jetbrains.teamcity.resources;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import jetbrains.buildServer.util.FileUtil;

import org.apache.log4j.Logger;

import com.jetbrains.teamcity.Storage;
import com.jetbrains.teamcity.Util;

public class TCWorkspace {
	
	private static Logger LOGGER = Logger.getLogger(TCWorkspace.class) ;
	
	public static final String TCC_ADMIN_FILE = ".tcc";
	
	private HashMap<File, AdminFile> myCache = new HashMap<File, AdminFile>();

	private File myRootFolder;

	private AdminFile myGlobalAdmin;
	
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
			myGlobalAdmin = new AdminFile(defaultConfig);
		} else {
			LOGGER.debug(MessageFormat.format("Default Admin file \"{0}\" is not found", myDefaultConfigFile));			
		}
	}
	
	public TCWorkspace(final File rootFolder, final File adminFile){
		this(rootFolder);
		if(adminFile != null && adminFile.exists()){
			myGlobalAdmin = new AdminFile(adminFile);
		} else {
			LOGGER.debug(MessageFormat.format("Admin file \"{0}\" is not found", adminFile));			
		}
	}
	
	static AdminFile getAdminFileFor(final File local) throws IllegalArgumentException {
		if(local == null){
			throw new IllegalArgumentException("File cannot be null");
		}
		//per-folder search
		final File folder = local.getParentFile();
		if(folder != null){
			try {
				final File admin = new File(folder.getCanonicalFile().getAbsoluteFile(), TCC_ADMIN_FILE);
				if(admin != null && admin.exists()){
					return new AdminFile(admin);
				} else {
					return getAdminFileFor(folder);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return null;
	}
	
	public ITCResource getResource(final File local) throws IllegalArgumentException {
		if(local == null){
			throw new IllegalArgumentException("File cannot be null");
		}
		try{
			//look into cache(cache keep file for files resides in same folder only)
			AdminFile admin = myCache.get(local.getParentFile()); 
			if(admin == null){
				//seek for uncached
				admin = getAdminFileFor(local);
				if(admin == null){
					//look into Global
					admin = myGlobalAdmin;
					if(myGlobalAdmin == null){
						LOGGER.debug(MessageFormat.format("Neither Local nor Global admin files found for \"{0}\"", local));
						return null;
					}
				}
			}
			if(admin != null){
				//cache found
				myCache.put(local.getParentFile(), admin);
				final Matching matching = admin.getMatching(local);
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

		} catch (IOException e){
			throw new IllegalArgumentException(e);
		}
	}
	
	static class AdminFile {
		
		private static final Comparator<String> PATH_SORTER = new Comparator<String>(){
			public int compare(final String key0, final String key1) {
				return key1.toLowerCase().compareTo(key0.toLowerCase());
			}};
		
		File myFile;
		private List<String> myItems;
		private TreeMap<String, String> myRulesMap = new TreeMap<String, String>(PATH_SORTER);

		AdminFile(File file){
			try {
				myFile = file;
				//parse content
				myItems = FileUtil.readFile(myFile);
				if(myItems.isEmpty()){
					throw new IllegalArgumentException(MessageFormat.format("\"{0}\" is empty", myFile));
				}
				//looking for default
				for(final String item : myItems){
					final String[] columns = item.trim().split("=");
					if (columns.length < 2) {
						throw new IllegalArgumentException(MessageFormat.format("\"{0}\" format is wrong", myFile));		
					}
					
					final String path = columns[0];
					final String tcid = FileUtil.removeTailingSlash(columns[1]);//slash will be added to Url later
					//absolute or not
					File ruleContainer = new File(path);
					if(!ruleContainer.isAbsolute()){
						ruleContainer = new File(myFile.getParentFile().getAbsoluteFile(), path);
					}
					myRulesMap.put(Util.toPortableString(ruleContainer.getCanonicalFile().getAbsolutePath()), tcid);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		
		public Matching getMatching(final File file) throws IOException {
			final String absolutePath = Util.toPortableString(file.getCanonicalFile().getAbsolutePath());
			for(final String path : myRulesMap.keySet()){
				if(absolutePath.startsWith(path)){
					final String prefix = myRulesMap.get(path);
					final String relativePath = absolutePath.substring(path.length() + 1/*do not include slash*/, absolutePath.length());//TODO: check +1 
					return new Matching(prefix, relativePath);
				}
			}
			return null;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj instanceof AdminFile){
				return myFile.equals(((AdminFile) obj).myFile);
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return myFile.hashCode();
		}
		
	}
	
	static class Matching {
		private String myPrefix;
		private String myRelativePath;
		
		Matching(final String prefix, final String relativePath){
			myPrefix = prefix;
			myRelativePath = relativePath;
		}
		
		public String getTCID() {
			return myPrefix;
		}
		
		public String getRelativePath() {
			return myRelativePath;
		}
		
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

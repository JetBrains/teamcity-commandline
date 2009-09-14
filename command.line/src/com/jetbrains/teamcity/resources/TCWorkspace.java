package com.jetbrains.teamcity.resources;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

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
	
	public TCWorkspace(final File rootFolder, final File adminFile){
		if(rootFolder == null){
			throw new IllegalArgumentException("Root directory cannot be null");
		}
		myRootFolder = rootFolder;
		
		//setup global admin
		if(adminFile != null && adminFile.exists()){
			myGlobalAdmin = new AdminFile(adminFile);
		} else {
			final String home = System.getProperty("user.home"); //$NON-NLS-1$
			final String myDefaultConfigFile = home + File.separator + Storage.TC_CLI_HOME + File.separator + ".tcc.global.config";
			LOGGER.debug(MessageFormat.format("Could not find \"{0}\" admin file. Will check for Default one: {1}", adminFile, myDefaultConfigFile));
			final File defaultConfig = new File(myDefaultConfigFile);
			if(defaultConfig.exists()){
				myGlobalAdmin = new AdminFile(defaultConfig);
			}
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
		//look into cache
		AdminFile admin = myCache.get(local.getParentFile()); 
		if(admin != null){
			return new TCResource(local, admin.getRepositoryLocation(local));
		}
		//seek for uncached
		admin = getAdminFileFor(local);
		if(admin == null){
			//look into Global
			if(myGlobalAdmin != null){
				return new TCResource(local, myGlobalAdmin.getRepositoryLocation(local));
			}
			LOGGER.debug(MessageFormat.format("Neither Local nor Global admin files found for \"{1}\"", local));
			return null;
		}
		//cache found
		myCache.put(local.getParentFile(), admin);
		return new TCResource(local, admin.getRepositoryLocation(local));
	}
	
	static class AdminFile {
		
		File myFile;
		private List<String> myItems;
		private String myDefaultPrefix;

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
					if(columns.length<2){
						throw new IllegalArgumentException(MessageFormat.format("\"{0}\" format is wrong", myFile));		
					}
					
					if(".".equals(columns[0].trim())){
						myDefaultPrefix = FileUtil.removeTailingSlash(Util.toPortableString(columns[1].trim()));
						break;
					}
				}
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		
		public String getRepositoryLocation(final File local) throws IllegalArgumentException {
			try {
				//TODO: check pattern from file
				final String pathFromHere = Util.getRelativePath(myFile.getParentFile().getAbsoluteFile(), local.getAbsoluteFile());
				final String prefix = getMatching(pathFromHere);
				return MessageFormat.format("{0}/{1}", prefix, pathFromHere);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}

			
		}
		
		private String getMatching(final String pathFromHere) {
			return myDefaultPrefix;
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

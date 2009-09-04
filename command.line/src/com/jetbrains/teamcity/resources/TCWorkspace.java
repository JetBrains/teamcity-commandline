package com.jetbrains.teamcity.resources;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import jetbrains.buildServer.util.FileUtil;

import com.jetbrains.teamcity.Util;

public class TCWorkspace {
	
	public static final String TCC_ADMIN_FILE = ".tcc";
	
	private HashMap<File, AdminFile> myCache = new HashMap<File, AdminFile>();
	
	private TCWorkspace(){
	}
	
	public static TCWorkspace getWorkspace(){
		return new TCWorkspace();
	}

	static AdminFile getAdminFileFor(final File local) throws IllegalArgumentException {
		if(local == null){
			throw new IllegalArgumentException("File cannot be null");
		}
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
			throw new IllegalArgumentException(MessageFormat.format("Could not find \"{0}\" for \"{1}\"", TCC_ADMIN_FILE, local));
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
		
	}
	
	
	
}

package com.jetbrains.teamcity.resources;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import jetbrains.buildServer.util.FileUtil;

import com.jetbrains.teamcity.Util;

public class FileBasedMatcher implements ITCResourceMatcher {
	
	private static final Comparator<String> PATH_SORTER = new Comparator<String>(){
		public int compare(final String key0, final String key1) {
			return key1.toLowerCase().compareTo(key0.toLowerCase());
		}};
	
	File myFile;
	private List<String> myItems;
	private TreeMap<String, String> myRulesMap = new TreeMap<String, String>(PATH_SORTER);

	public FileBasedMatcher(final File file){
		if (file == null || !file.exists()) {
			throw new IllegalArgumentException(MessageFormat.format("File is null or not extists: \"{0}\"", file));
		}
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
	
	public Matching getMatching(final File file) throws IllegalArgumentException {
		try{
			final String absolutePath = Util.toPortableString(file.getCanonicalFile().getAbsolutePath());
			for(final String path : myRulesMap.keySet()){
				if(absolutePath.startsWith(path)){
					final String prefix = myRulesMap.get(path);
					final String relativePath = absolutePath.substring(path.length() + 1/*do not include slash*/, absolutePath.length());//TODO: check +1 
					return new MatchingImpl(prefix, relativePath);
				}
			}
			return null;
			
		} catch (IOException e){
			throw new IllegalArgumentException(e);
			
		}
	}
	
	static class MatchingImpl implements ITCResourceMatcher.Matching {
		private String myPrefix;
		private String myRelativePath;
		
		MatchingImpl(final String prefix, final String relativePath){
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
	
	

}

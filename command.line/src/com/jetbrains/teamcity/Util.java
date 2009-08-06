package com.jetbrains.teamcity;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import jetbrains.buildServer.util.FileUtil;

public class Util {
	
	public static IFileFilter CVS_FILES_FILTER = new CVSFilter(); 
	
	public static IFileFilter SVN_FILES_FILTER = new SVNFilter();
	
	private static Pattern ASTERISK_PATTERN = Pattern.compile(".*"); //$NON-NLS-1$
	
	public static String getArgumentValue(final String[] args, final String ...arguments) {
		for(int i = 0; i< args.length; i++){
			for(String argument : arguments){
				if(argument != null){
					if(args[i].toLowerCase().trim().equals(argument.toLowerCase().trim())){
						return args[i + 1].trim();
					}
				}
			}
		}
		return null;
	}
	
	public static boolean hasArgument(final String[] args, final String ...arguments) {
		for(int i = 0; i< args.length; i++){
			for(String argument : arguments){
				if(argument != null){
					if(args[i].toLowerCase().trim().equals(argument.toLowerCase().trim())){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static String getRelativePath(final File root, final File to) throws IOException, IllegalArgumentException {
		if(root == null || to==null){
			throw new IllegalArgumentException(MessageFormat.format("Null is not supported as argument: {0}, {1}", root, to)); //$NON-NLS-1$
		}
		if(to.isAbsolute()){
			return FileUtil.getRelativePath(root, to).replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			return to.getPath().replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public static Collection<File> getFiles(final String path) throws IllegalArgumentException {
		final File simpleFile = new File(path).getAbsoluteFile();
		if(simpleFile.exists() && simpleFile.isFile()){
			return Collections.<File>singletonList(simpleFile);
		} else if (simpleFile.exists() && simpleFile.isDirectory()){
			final ArrayList<File> list = new ArrayList<File>();
			FileUtil.collectMatchedFiles(simpleFile, ASTERISK_PATTERN, list);
			return list;
		} else if (hasFilePatterns(path)){
			final ArrayList<File> list = new ArrayList<File>();
			FileUtil.collectMatchedFiles(simpleFile, Pattern.compile(path), list);
			return list;
		}
		return Collections.singletonList(simpleFile);//let it be 
	}

	static boolean hasFilePatterns(String path) {
		return path.contains("*") || path.contains("!");  //$NON-NLS-1$//$NON-NLS-2$
	}

	public static Collection<File> getFiles(final File file){
		if(!file.exists()){
			throw new IllegalArgumentException(MessageFormat.format("File is not found \"{0}\"", file.getAbsolutePath())); //$NON-NLS-1$
		}
		try {
			final List<String> content = FileUtil.readFile(file);
			final HashSet<File> files = new HashSet<File>(content.size());
			for(String path : content){
				if(path.trim().length()>0){
					files.addAll(getFiles(path));
				}
			}
			return files;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public interface IFileFilter {
		public Collection<File> accept(final Collection<File> files);
	}
	
	private static class CVSFilter implements IFileFilter {
		
		public Collection<File> accept(Collection<File> files) {
			final HashSet<File> result = new HashSet<File>();
			for(final File file : files){
				final String normalPath = file.getPath().toLowerCase().replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
				if(!normalPath.endsWith("cvs/entries") && !normalPath.endsWith("cvs/repository") && !normalPath.endsWith("cvs/root")){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					result.add(file);
				}
			}
			return result;
		}
	}
	
	private static class SVNFilter implements IFileFilter {
		
		public Collection<File> accept(Collection<File> files) {
			final HashSet<File> result = new HashSet<File>();
			for(final File file : files){
				final String normalPath = file.getPath().toLowerCase();
				if(!normalPath.contains(".svn")){ //$NON-NLS-1$
					result.add(file);
				}
			}
			return result;
		}
	}

	public static Throwable getRootCause(Throwable throwable) {
		Throwable cause = throwable.getCause();
		if (cause != null) {
			throwable = cause;
			while ((throwable = throwable.getCause()) != null) {
				cause = throwable;
			}
			return cause;
		}
		return throwable;
	}
	
}

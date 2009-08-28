package com.jetbrains.teamcity;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
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
		try {
			final File simpleFile = new File(path).getCanonicalFile().getAbsoluteFile();
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
		} catch (IOException e) {
			throw new IllegalArgumentException(MessageFormat.format("Wrong path passed: {0}", path));
		}
	}

	static boolean hasFilePatterns(String path) {
		return path.contains("*") || path.contains("!");  //$NON-NLS-1$//$NON-NLS-2$
	}

	public static Collection<File> getFiles(final File file){
		if(!file.exists()){
			throw new IllegalArgumentException(MessageFormat.format("File is not found \"{0}\"", file.getAbsolutePath())); //$NON-NLS-1$
		}
		if(file.length() == 0){
			throw new IllegalArgumentException(MessageFormat.format("File \"{0}\" is empty", file.getAbsolutePath())); //$NON-NLS-1$	
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

	public static class StringTable{
		
		private String[] myHeader;
		private LinkedList<String[]> rows = new LinkedList<String[]>();
		private int myNumColumns;

		public StringTable(final String[] header){
			if(header == null || header.length == 0){
				throw new IllegalArgumentException("Header cannot be null or empty");
			}
			myHeader = header;
			myNumColumns = myHeader.length;
		}
		
		public StringTable(final int numColumns){
			myNumColumns = numColumns;
		}
		
		/**
		 * @param tabbedHeader Plain string represents Column's headers devided by '\t' 
		 */
		public StringTable(final String tabbedHeader){
			this(tabbedHeader.split("\t"));
		}
		
		/**
		 * 
		 * @param tabbedRow Plain string represents row. columns must be devided by '\t'
		 */
		public void addRow(final String tabbedRow){
			final String[] splited = tabbedRow.split("\t");
			if(splited.length == myNumColumns){
				addRow(splited);
				
			} else if(splited.length < myNumColumns){
				final String[] paddedRow = new String[myNumColumns];
				Arrays.fill(paddedRow, "");
				for(int i = 0; i< splited.length; i++){
					paddedRow[i] = splited[i];
				}
				addRow(paddedRow);
			} else {
				final String[] trimmedRow = new String[myNumColumns];
				for(int i = 0; i< myNumColumns; i++){
					trimmedRow[i] = splited[i];
				}
				addRow(trimmedRow);
			}
		}
	
		public void addRow(final String[] row){
			if(row == null || row.length != myNumColumns){
				throw new IllegalArgumentException(MessageFormat.format("Row is null or size differs to Header {1}: {0}", Arrays.toString(row), myNumColumns));
			}
			rows.add(row);
		}
		
		public String toString(){
			final LinkedList<String[]> buffer = new LinkedList<String[]>(rows);
			if(myHeader != null){
				buffer.add(0, myHeader);
			}

			// collect max lengths of columns
			final int[] maxSizes = new int[myNumColumns];
			for (final String[] row : buffer) {
				for (int i = 0; i < myNumColumns; i++) {
					if(row[i] != null){
						maxSizes[i] = Math.max(row[i].length(), maxSizes[i]);
					}
				}
			}
			//so, let's format result according to maxSizes...
			final StringBuffer result = new StringBuffer();
			for (final String[] row : buffer) {
				for (int i = 0; i < myNumColumns; i++) {
					final String column = row[i] != null? row[i].replace("\n", "\\") : "";
					final int maxStringLenght = maxSizes[i];
					result.append(String.format("%1$-" + ((i != (myNumColumns - 1)) ? (maxStringLenght + 1) : maxStringLenght) + "s", column));
					
				}
				result.append("\n");
			}
			return result.toString();
		}
		
	}
	
	public static void main(String[] args){
		{
			final StringTable table = new StringTable(new String[] {"Fist Column", "2", "The last"});
			table.addRow(new String[] {"1", "qwiueiqwiei, qweiuqwieiqiwei", "a"});
			table.addRow(new String[] {"2", "qweiuqwieiqiwei", "asdsdsdsd, sdsdsds\ndsdsdsdsd"});
			table.addRow(new String[] {"3", "q", "asdsdsdsd"});
			System.out.println(table);
		}
		
		{
			final StringTable table = new StringTable("Fist Column\t2\tThe last");
			table.addRow("1\tqwiueiqwiei, qweiuqwieiqiwei\ta");
			table.addRow("2\tqweiuqwieiqiwei\tasdsdsdsd, sdsdsdsdsdsdsdsd");
			table.addRow("3\tq\tasdsdsdsd");
			System.out.println(table);
		}

		{
			final StringTable table = new StringTable(3);
			table.addRow("1\tqwiueiqwiei, qweiuqwieiqiwei\ta");
			table.addRow("2\tqweiuqwieiqiwei\tasdsdsdsd, sdsdsdsdsdsdsdsd");
			table.addRow("3\tq\tasdsdsdsd");
			System.out.println(table);
		}
		
		
	}
	
	public static String readConsoleInput(String prompth) {
		if(prompth != null){
			System.out.print(prompth);
		}
		final Scanner scanner = new Scanner(System.in);
		return scanner.nextLine();
	}
	
	public static String readConsoleInput() {
		return readConsoleInput(null);
	}
	
	
}

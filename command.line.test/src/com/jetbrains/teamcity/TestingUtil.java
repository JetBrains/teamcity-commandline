package com.jetbrains.teamcity;

import java.io.File;
import java.io.IOException;

import jetbrains.buildServer.util.FileUtil;

import org.junit.Test;

public class TestingUtil {

	@Test
	public void dummy() {
		
	}
	
	public static File getFSRoot(){
		for(File root :File.listRoots()){
			if(root.canRead()){
				return root;
			}
		}
		return null;
	}
	
	public static File createFS() throws IOException {
		//dirs
		File root = new File("rootTestFolder");
		root.mkdir();
		final File javaFolder = new File(root, "java");
		javaFolder.mkdir();
		
		final File resourceFolderInJavaFolder = new File(javaFolder, "resources");
		resourceFolderInJavaFolder.mkdir();
		
		final File cppFolder = new File(root, "cpp");
		cppFolder.mkdir();
		final File resourceFolderInCppFolder = new File(cppFolder, "resources");
		resourceFolderInCppFolder.mkdir();
		
		//files
		new File(javaFolder, "1.java").createNewFile();
		new File(resourceFolderInJavaFolder, "2.java").createNewFile();
		new File(resourceFolderInJavaFolder, "java.resources").createNewFile();
		new File(cppFolder, "1.cpp").createNewFile();
		new File(resourceFolderInCppFolder, "cpp.resources").createNewFile();
		
		return root;
		
	}
	
	public static void releaseFS(File root){
		FileUtil.delete(root);
	}
	
	
}

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
		//clean to be sure
		FileUtil.delete(root);
		
		root.mkdir();
		System.out.println(String.format("TestingUtil.createFS(): %s created", root));
		
		final File javaFolder = new File(root, "java");
		javaFolder.mkdir();
		System.out.println(String.format("TestingUtil.createFS(): %s created", javaFolder));
		
		final File resourceFolderInJavaFolder = new File(javaFolder, "resources");
		resourceFolderInJavaFolder.mkdir();
		System.out.println(String.format("TestingUtil.createFS(): %s created", resourceFolderInJavaFolder));
		
		final File cppFolder = new File(root, "cpp");
		cppFolder.mkdir();
		System.out.println(String.format("TestingUtil.createFS(): %s created", cppFolder));
		
		final File resourceFolderInCppFolder = new File(cppFolder, "resources");
		resourceFolderInCppFolder.mkdir();
		System.out.println(String.format("TestingUtil.createFS(): %s created", resourceFolderInCppFolder));
		
		//files
		final File java_one_file = new File(javaFolder, "1.java");
		java_one_file.createNewFile(); FileUtil.writeFile(java_one_file, "1.java");
		System.out.println(String.format("TestingUtil.createFS(): %s created", java_one_file));
		
		final File java_two_file = new File(resourceFolderInJavaFolder, "2.java");
		java_two_file.createNewFile(); FileUtil.writeFile(java_two_file, "2.java");
		System.out.println(String.format("TestingUtil.createFS(): %s created", java_two_file));
		
		final File java_res_file = new File(resourceFolderInJavaFolder, "java.resources");
		java_res_file.createNewFile(); FileUtil.writeFile(java_res_file, "java.resources");
		System.out.println(String.format("TestingUtil.createFS(): %s created", java_res_file));
		
		final File cpp_file = new File(cppFolder, "1.cpp");
		cpp_file.createNewFile(); FileUtil.writeFile(cpp_file, "1.cpp");
		System.out.println(String.format("TestingUtil.createFS(): %s created", cpp_file));
		
		final File cpp_ress_file = new File(resourceFolderInCppFolder, "cpp.resources");
		cpp_ress_file.createNewFile(); FileUtil.writeFile(cpp_ress_file, "1.cpp.resources");
		System.out.println(String.format("TestingUtil.createFS(): %s created", cpp_ress_file));
		
		return root;
		
	}
	
	public static void releaseFS(File root){
		FileUtil.delete(root);
	}
	
	
}

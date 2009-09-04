package com.jetbrains.teamcity.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import jetbrains.buildServer.util.FileUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jetbrains.teamcity.TestingUtil;


public class TCWorkspaceTest {
	
	private static TCWorkspace ourTestWorkspace;

	@BeforeClass
	public static void setup(){
		ourTestWorkspace = TCWorkspace.getWorkspace();
	}
	
	@AfterClass
	public static void shutdown(){
		
	}
	
	@Test
	public void getAdminFileFor_error_handling() throws Exception {
		//null
		try{
			TCWorkspace.getAdminFileFor(null);
			assertTrue("Exception must be thrown", true);
		} catch (IllegalArgumentException e){
			//okj
		}
		//root 
		final File fsRoot = TestingUtil.getFSRoot();
		assertNull(TCWorkspace.getAdminFileFor(fsRoot));
		//file in root 
		TCWorkspace.getAdminFileFor(new File(fsRoot, "file.txt"));// no exception
		
	}
	
	@Test
	public void getAdminFileFor_functionality() throws Exception {
		final File root = TestingUtil.createFS();
		try{
			
			File file = new File(root, "java/resources/java.resources");
			assertNull("Admin file found for: " + file, TCWorkspace.getAdminFileFor(file));// still_not_created
			
			final File rootAdminFile = new File(root, TCWorkspace.TCC_ADMIN_FILE);
			FileUtil.writeFile(rootAdminFile, ".=//depo/test/\n");
			
			File java = new File(root, "1.java");
			assertNotNull("No Admin file found for: " + java, TCWorkspace.getAdminFileFor(java));// the_same_place
			
			File javaResource = new File(root, "java/resources/java.resources");
			assertNotNull("No Admin file found for: " + javaResource, TCWorkspace.getAdminFileFor(javaResource));// in_hierarchy
			
			//let's create overriding .tcc
			final File cppRootAdminFile = new File(root, "cpp/" + TCWorkspace.TCC_ADMIN_FILE).getAbsoluteFile();
			FileUtil.writeFile(cppRootAdminFile, ".=//depo/test/\n");
			
			File cppResources = new File(root, "cpp/resources/cpp.resources");
			assertEquals("Unexpected Admin file found for: " + cppResources, cppRootAdminFile, TCWorkspace.getAdminFileFor(cppResources).myFile);// in_hierarchy
			
		} finally {
			TestingUtil.releaseFS(root);
		}
	}
	
	@Test
	public void getResource_error_handling() throws Exception {
		//null
		try{
			ourTestWorkspace.getResource(null);
			assertTrue("Exception must be thrown", true);
		} catch (IllegalArgumentException e){
			//okj
		}
	}
	
	@Test
	public void getResource_functionality() throws Exception {
		final File root = TestingUtil.createFS();
		try{
			final File rootAdminFile = new File(root, TCWorkspace.TCC_ADMIN_FILE);
			FileUtil.writeFile(rootAdminFile, ".=//depo/test/\n");
			
			//simple
			File java = new File(root, "1.java");
			ITCResource itcResource = ourTestWorkspace.getResource(java);
			assertNotNull("No ITCResource created for: " + java, itcResource);
			assertEquals("//depo/test/1.java", itcResource.getRepositoryPath());
			//in hierarchy
			File javaResource = new File(root, "java/resources/java.resources");
			itcResource = ourTestWorkspace.getResource(javaResource);
			assertNotNull("No ITCResource created for: " + javaResource, itcResource);// in_hierarchy
			assertEquals("//depo/test/java/resources/java.resources", itcResource.getRepositoryPath());// in_hierarchy
			//overriding
			final File cppRootAdminFile = new File(root, "cpp/" + TCWorkspace.TCC_ADMIN_FILE).getAbsoluteFile();
			FileUtil.writeFile(cppRootAdminFile, ".=//depo/test/CPLUSPLUS/src\n");
			File cppResource = new File(root, "cpp/resources/cpp.resources");
			itcResource = ourTestWorkspace.getResource(cppResource);
			assertNotNull("No ITCResource created for: " + cppResource, itcResource);// in_hierarchy
			assertEquals("//depo/test/CPLUSPLUS/src/resources/cpp.resources", itcResource.getRepositoryPath());// in_hierarchy
			
			
		} finally {
			TestingUtil.releaseFS(root);
		}
	}
	
	
	

}

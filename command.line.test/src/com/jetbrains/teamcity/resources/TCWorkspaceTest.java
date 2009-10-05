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
		ourTestWorkspace = new TCWorkspace(new File(System.getProperty("user.dir")), null);
	}
	
	@AfterClass
	public static void shutdown(){
		
	}
	
	@Test
	public void getAdminFileFor_error_handling() throws Exception {
		//null
		try{
			TCWorkspace.getMatcherFor(null);
			assertTrue("Exception must be thrown", true);
		} catch (IllegalArgumentException e){
			//okj
		}
		//root 
		final File fsRoot = TestingUtil.getFSRoot();
		assertNull(TCWorkspace.getMatcherFor(fsRoot));
		//file in root 
		TCWorkspace.getMatcherFor(new File(fsRoot, "file.txt"));// no exception
		
	}
	
	@Test
	public void getAdminFileFor_functionality() throws Exception {
		final File root = TestingUtil.createFS();
		try{
			
			File file = new File(root, "java/resources/java.resources");
			assertNull("Admin file found for: " + file, TCWorkspace.getMatcherFor(file));// still_not_created
			
			final File rootAdminFile = new File(root, TCWorkspace.TCC_ADMIN_FILE);
			FileUtil.writeFile(rootAdminFile, ".=//depo/test/\n");
			
			File java = new File(root, "1.java");
			assertNotNull("No Admin file found for: " + java, TCWorkspace.getMatcherFor(java));// the_same_place
			
			File javaResource = new File(root, "java/resources/java.resources");
			assertNotNull("No Admin file found for: " + javaResource, TCWorkspace.getMatcherFor(javaResource));// in_hierarchy
			
		} finally {
			TestingUtil.releaseFS(root);
		}
	}
	
	@Test
	public void getMatching_relative() throws Exception {
		final File root = TestingUtil.createFS();
		final File globalAdminFile = new File(root, TCWorkspace.TCC_ADMIN_FILE + ".test.admin." + System.currentTimeMillis());
		try{
			//create global Admin
			FileUtil.writeFile(globalAdminFile, ".=//depo/test/dot\n" +
					"cpp=//depo/test/cpp_root\n" +
					"cpp/resources=//depo/test/cpp_resources\n");
			final ITCResourceMatcher admin = new FileBasedMatcher(globalAdminFile);
			
			//overriding
			File cpp = new File(root, "cpp/1.cpp");
			File cppResource = new File(root, "cpp/resources/cpp.resources");
			
			ITCResourceMatcher.Matching cppMatching = admin.getMatching(cpp);
			ITCResourceMatcher.Matching resCppMatching = admin.getMatching(cppResource);
			
			assertNotNull(cppMatching);
			assertEquals("//depo/test/cpp_root", cppMatching.getTCID());// in_hierarchy
			
			assertNotNull(resCppMatching);
			assertEquals("//depo/test/cpp_resources", resCppMatching.getTCID());// in_hierarchy
			
		} finally {
			globalAdminFile.deleteOnExit();
			TestingUtil.releaseFS(root);
		}
	}
	
	@Test
	public void getResource_error_handling() throws Exception {
		//null
		try{
			ourTestWorkspace.getTCResource(null);
			assertTrue("Exception must be thrown", true);
		} catch (IllegalArgumentException e){
			//okj
		}
	}
	
	@Test
	public void getResource_per_folder_relative_path() throws Exception {
		final File root = TestingUtil.createFS();
		try{
			final File rootAdminFile = new File(root, TCWorkspace.TCC_ADMIN_FILE);
			FileUtil.writeFile(rootAdminFile, ".=//depo/test/\n");
			getResource_test_paths(root);
		} finally {
			TestingUtil.releaseFS(root);
		}
	}
	
	@Test
	public void getResource_per_folder_absolute_path() throws Exception {
		final File root = TestingUtil.createFS();
		try{
			final File rootAdminFile = new File(root, TCWorkspace.TCC_ADMIN_FILE);
			FileUtil.writeFile(rootAdminFile, new File(".").getCanonicalFile().getAbsolutePath() + "=//depo/test/\n");
			getResource_test_paths(root);
		} finally {
			TestingUtil.releaseFS(root);
		}
	}
	
	
	private void getResource_test_paths(final File root) throws Exception {
			//simple
			File java = new File(root, "1.java");
			ITCResource itcResource = ourTestWorkspace.getTCResource(java);
			assertNotNull("No ITCResource created for: " + java, itcResource);
			assertEquals("//depo/test/1.java", itcResource.getRepositoryPath());
			//in hierarchy
			File javaResource = new File(root, "java/resources/java.resources");
			itcResource = ourTestWorkspace.getTCResource(javaResource);
			assertNotNull("No ITCResource created for: " + javaResource, itcResource);// in_hierarchy
			assertEquals("//depo/test/java/resources/java.resources", itcResource.getRepositoryPath());// in_hierarchy
			//overriding
			final File cppRootAdminFile = new File(root, "cpp/" + TCWorkspace.TCC_ADMIN_FILE).getAbsoluteFile();
			FileUtil.writeFile(cppRootAdminFile, ".=//depo/test/CPLUSPLUS/src\n");
			File cppResource = new File(root, "cpp/resources/cpp.resources");
			itcResource = ourTestWorkspace.getTCResource(cppResource);
			assertNotNull("No ITCResource created for: " + cppResource, itcResource);// in_hierarchy
			assertEquals("//depo/test/CPLUSPLUS/src/resources/cpp.resources", itcResource.getRepositoryPath());// in_hierarchy
	}
	
	@Test
	public void getResource_global_absolute_path() throws Exception {
		final File root = TestingUtil.createFS();
		final File globalAdminFile = new File(TCWorkspace.TCC_GLOBAL_ADMIN_FILE);
		try{
			//create global Admin
			final File testRootFolder = root.getCanonicalFile();
			FileUtil.writeFile(globalAdminFile, testRootFolder.getAbsolutePath() + "=//depo/test/\n");
			final TCWorkspace workspace = new TCWorkspace(testRootFolder, null/*new FileBasedMatcher(globalAdminFile)*/);
			
			//simple
			File java = new File(root, "1.java");
			ITCResource itcResource = workspace.getTCResource(java);
			assertNotNull("No ITCResource created for: " + java, itcResource);
			assertEquals("//depo/test/1.java", itcResource.getRepositoryPath());
			//in hierarchy
			File javaResource = new File(root, "java/resources/java.resources");
			itcResource = workspace.getTCResource(javaResource);
			assertNotNull("No ITCResource created for: " + javaResource, itcResource);// in_hierarchy
			assertEquals("//depo/test/java/resources/java.resources", itcResource.getRepositoryPath());// in_hierarchy
		} finally {
			globalAdminFile.deleteOnExit();
			TestingUtil.releaseFS(root);
		}
	}
	
	@Test
	public void getResource_global_overrided_with_per_folder() throws Exception {
		final File root = TestingUtil.createFS();
		final File globalAdminFile = new File(TCWorkspace.TCC_GLOBAL_ADMIN_FILE);
		try{
			//create global Admin
			FileUtil.writeFile(globalAdminFile, root.getCanonicalFile().getAbsolutePath() + "=//depo/test/\n");
			final TCWorkspace workspace = new TCWorkspace(root, null);
			
			//overriding
			final File cppRootAdminFile = new File(root, "cpp/" + TCWorkspace.TCC_ADMIN_FILE).getAbsoluteFile();
			FileUtil.writeFile(cppRootAdminFile, ".=//depo/test/CPLUSPLUS/src\n");
			File cppResource = new File(root, "cpp/resources/cpp.resources");
			ITCResource itcResource = workspace.getTCResource(cppResource);
			assertNotNull("No ITCResource created for: " + cppResource, itcResource);// in_hierarchy
			assertEquals("//depo/test/CPLUSPLUS/src/resources/cpp.resources", itcResource.getRepositoryPath());// in_hierarchy
		} finally {
			globalAdminFile.deleteOnExit();
			TestingUtil.releaseFS(root);
		}
	}
	


}

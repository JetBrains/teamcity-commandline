package com.jetbrains.teamcity.resources;

import com.jetbrains.teamcity.TestingUtil;
import java.io.File;
import java.io.IOException;
import jetbrains.buildServer.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


public class TCWorkspaceTest {
	
	private static TCWorkspace ourTestWorkspace;
	private File root;

	@BeforeClass
	public static void setup() {
		ourTestWorkspace = new TCWorkspace();
	}

	@Before
	public void setUp() throws IOException {
		root = TestingUtil.createFS();

		File file = new File(TCWorkspace.TCC_GLOBAL_ADMIN_FILE + ".test");
		file.mkdirs();
		FileUtil.delete(file);
	}

	@After
	public void tearDown() {
		TestingUtil.releaseFS(root);
	}

	@Test
	public void getAdminFileFor_error_handling() throws Exception {

		//root
		File root = TestingUtil.getFSRoot();
		assertNull(TCWorkspace.getMatcherFor(root));
		//file in root 
		TCWorkspace.getMatcherFor(new File(root, "file.txt"));// no exception
		
	}
	
	@Test
	public void getAdminFileFor_functionality() throws Exception {

			File file = new File(root, "java/resources/java.resources");
			assertNull("Admin file found for: " + file, TCWorkspace.getMatcherFor(file));// still_not_created
			
			final File rootAdminFile = new File(root, TCWorkspace.TCC_ADMIN_FILE);
            FileUtil.writeFileAndReportErrors(rootAdminFile, ".=//depo/test/\n");

			File java = new File(root, "1.java");
			assertNotNull("No Admin file found for: " + java, TCWorkspace.getMatcherFor(java));// the_same_place
			
			File javaResource = new File(root, "java/resources/java.resources");
			assertNotNull("No Admin file found for: " + javaResource, TCWorkspace.getMatcherFor(javaResource));// in_hierarchy
			
	}
	
	@Test
	public void getMatching_relative() throws Exception {
		final File globalAdminFile = new File(root, TCWorkspace.TCC_ADMIN_FILE + ".test.admin." + System.currentTimeMillis());
		try{
			//create global Admin
            FileUtil.writeFileAndReportErrors(globalAdminFile, ".=//depo/test/dot\n" +
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
		}
	}

	@Test
	public void testNPE_when_matching_TW_58901() throws IOException {
		FileUtil.writeFileAndReportErrors(new File(root, TCWorkspace.TCC_ADMIN_FILE),
										  "u-boot=perforce://perforce:1666:////Products/TRUNK/3800Core/u-boot-3802\n");
		final File local = new File(root, "u-boot");
		local.mkdir();

		final ITCResource tcResource = new TCWorkspace().getTCResource(local);
		assertEquals(tcResource.getLocal().getAbsoluteFile(), local.getAbsoluteFile());
		assertEquals(tcResource.getRepositoryPath(), "perforce://perforce:1666:////Products/TRUNK/3800Core/u-boot-3802/");
	}
	
	@Test
	public void testNPE_when_matching_TW_58901_noMatch() throws IOException {
		FileUtil.writeFileAndReportErrors(new File(root, TCWorkspace.TCC_ADMIN_FILE),
										  "u-boot=perforce://perforce:1666:////Products/TRUNK/3800Core/u-boot-3802\n");
		final File local = new File(root, "u-boot1");
		local.mkdir();

		assertNull(new TCWorkspace().getTCResource(local));
	}

	@Test
	public void getResource_per_folder_relative_path() throws Exception {

			final File rootAdminFile = new File(root, TCWorkspace.TCC_ADMIN_FILE);
            FileUtil.writeFileAndReportErrors(rootAdminFile, ".=//depo/test/\n");
			getResource_test_paths(root);

	}
	
	@Test
	public void getResource_per_folder_absolute_path() throws Exception {

			final File rootAdminFile = new File(root, TCWorkspace.TCC_ADMIN_FILE);
            FileUtil.writeFileAndReportErrors(rootAdminFile, root.getCanonicalFile().getAbsolutePath() + "=//depo/test/\n");
			getResource_test_paths(root);

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
            FileUtil.writeFileAndReportErrors(cppRootAdminFile, ".=//depo/test/CPLUSPLUS/src\n");
			File cppResource = new File(root, "cpp/resources/cpp.resources");
			itcResource = ourTestWorkspace.getTCResource(cppResource);
			assertNotNull("No ITCResource created for: " + cppResource, itcResource);// in_hierarchy
			assertEquals("//depo/test/CPLUSPLUS/src/resources/cpp.resources", itcResource.getRepositoryPath());// in_hierarchy
	}
	
	@Test
	public void getResource_global_absolute_path() throws Exception {
		final File testGlobalAdminFile = new File(TCWorkspace.TCC_GLOBAL_ADMIN_FILE + ".test");
		try{
			//create global Admin
			final File testRootFolder = root.getCanonicalFile();
            FileUtil.writeFileAndReportErrors(testGlobalAdminFile, testRootFolder.getAbsolutePath() + "=//depo/test/\n");
			final TCWorkspace workspace = new TCWorkspace(null/*new FileBasedMatcher(globalAdminFile)*/){
				@Override
				protected File getGlobalAdminFile() {
					return testGlobalAdminFile;
				}
			};
			
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
			testGlobalAdminFile.deleteOnExit();
		}
	}
	
	@Test
	public void getResource_global_overrided_with_per_folder() throws Exception {
		final File testGlobalAdminFile = new File(TCWorkspace.TCC_GLOBAL_ADMIN_FILE + ".test");
		try{
			//create global Admin
            FileUtil.writeFileAndReportErrors(testGlobalAdminFile, root.getCanonicalFile().getAbsolutePath() + "=//depo/test/\n");
			final TCWorkspace workspace = new TCWorkspace(null){
				@Override
				protected File getGlobalAdminFile() {
					return testGlobalAdminFile;
				}
			};
			
			//overriding
			final File cppRootAdminFile = new File(root, "cpp/" + TCWorkspace.TCC_ADMIN_FILE).getAbsoluteFile();
            FileUtil.writeFileAndReportErrors(cppRootAdminFile, ".=//depo/test/CPLUSPLUS/src\n");
			File cppResource = new File(root, "cpp/resources/cpp.resources");
			ITCResource itcResource = workspace.getTCResource(cppResource);
			assertNotNull("No ITCResource created for: " + cppResource, itcResource);// in_hierarchy
			assertEquals("//depo/test/CPLUSPLUS/src/resources/cpp.resources", itcResource.getRepositoryPath());// in_hierarchy
		} finally {
			testGlobalAdminFile.deleteOnExit();
		}
	}
	


}

//package com.jetbrains.teamcity;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//
//import java.io.File;
//
//import jetbrains.buildServer.util.FileUtil;
//
//import org.junit.AfterClass;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//public class URLFactoryTest {
//
//private static File ourRootSvnEntries;
//private static File ourInnerSvnEntries;
//
//// rootTestFolder-
////                |.svn	
////                |java-
////	                    |1.java
////                      |resources-
////                                 |2.java
////                                 |1.resources	
////	              |cpp-
////	                   |.svn
////	                   |1.cpp
////	
//	@BeforeClass
//	public static void setup() throws Exception {
////		//dirs
//		final File rootFolder = new File("rootTestFolder");
//		rootFolder.mkdir();
//		final File firsChild = new File(rootFolder, "java");
//		firsChild.mkdir();
//		final File firsSvnChild = new File(rootFolder, URLFactory.SVNUrlFactory.SVN_FOLDER);
//		firsSvnChild.mkdir();
//		ourRootSvnEntries = new File(firsSvnChild, URLFactory.SVNUrlFactory.ENTRIES_FILE);
//		ourRootSvnEntries.createNewFile();
//		
//		final File firsFirstChild = new File(firsChild, "resources");
//		firsFirstChild.mkdir();
//		final File secondChild = new File(rootFolder, "cpp");
//		secondChild.mkdir();
//		final File secondSvnChild = new File(secondChild, URLFactory.SVNUrlFactory.SVN_FOLDER);
//		secondSvnChild.mkdir();
//		ourInnerSvnEntries = new File(secondSvnChild, URLFactory.SVNUrlFactory.ENTRIES_FILE);
//		ourInnerSvnEntries.createNewFile();
//		
//		//files
//		new File(firsChild, "1.java").createNewFile();
//		new File(firsFirstChild, "2.java").createNewFile();
//		new File(firsFirstChild, "1.resources").createNewFile();
//		new File(secondChild, "1.cpp").createNewFile();
//	}
//	
//	@AfterClass
//	public static void clean() throws Exception {
//		FileUtil.delete(new File("rootTestFolder"));
//	}
//	
//	@Test
//	public void SVNUrlFactory_getEntriesFile_controlledFolderTest() throws Exception {
//		final File entries = URLFactory.SVNUrlFactory.getEntriesFile("rootTestFolder/cpp/1.cpp");
//		assertNotNull(entries);
//		assertEquals(entries, ourInnerSvnEntries);
//	}
//	
//	@Test
//	public void SVNUrlFactory_getEntriesFile_newFolderTest() throws Exception {
//		final File entries = URLFactory.SVNUrlFactory.getEntriesFile("rootTestFolder/java/resources/2.java");
//		assertNotNull(entries);
//		assertEquals(entries, ourRootSvnEntries);
//	}
//	
//	
//
//}

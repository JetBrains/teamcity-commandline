package com.jetbrains.teamcity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;

import jetbrains.buildServer.util.FileUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class UtilTest {
	
	@BeforeClass
	public static void setup() throws Exception {
		//dirs
		final File rootFolder = new File("rootTestFolder");
		rootFolder.mkdir();
		final File firsChild = new File(rootFolder, "java");
		firsChild.mkdir();
		
		final File firsFirstChild = new File(firsChild, "resources");
		firsFirstChild.mkdir();
		final File secondChild = new File(rootFolder, "cpp");
		secondChild.mkdir();
		//files
		new File(firsChild, "1.java").createNewFile();
		new File(firsFirstChild, "2.java").createNewFile();
		new File(firsFirstChild, "1.resources").createNewFile();
		new File(secondChild, "1.cpp").createNewFile();
	}
	
	@AfterClass
	public static void clean() throws Exception {
		FileUtil.delete(new File("rootTestFolder"));
	}
	
	@Test
	public void hasArgument() throws Exception {
		assertTrue(Util.hasArgument(new String[] {"-one", "-two"}, "-one"));
		assertTrue(Util.hasArgument(new String[] {" -one ", "-two"}, " -one "));
		assertTrue(Util.hasArgument(new String[] {" -One "}, "-one "));
		assertTrue(Util.hasArgument(new String[] {"-one", "-two"}, "-one", "-two"));
		assertTrue(Util.hasArgument(new String[] {"-one", "-two"}, "-one", "-two", "-three"));
		assertFalse(Util.hasArgument(new String[] {"-one", "-two"}, "-tree"));
		assertFalse(Util.hasArgument(new String[] {"-one", "-two"}, (String)null));
		assertTrue(Util.hasArgument(new String[] {"-one", "-two"}, (String)null, "-one"));
	}
	
	@Test
	public void getArgument() throws Exception {
		assertEquals("one", Util.getArgumentValue(new String[] {"-one", "one", "-two", "two"}, "-one"));
		assertEquals("one", Util.getArgumentValue(new String[] {" -one", " one ", "-two", "two"}, "-one "));
		assertEquals("one", Util.getArgumentValue(new String[] {" -one", " one ", "-two", "two"}, (String)null, "-One"));
		assertEquals(null, Util.getArgumentValue(new String[] {" -one", " one ", "-two", "two"}, (String)null));
	}
	
	@Test
	public void getRelativePath() throws Exception {
		try{
			assertEquals(null, Util.getRelativePath(null, null));
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e){
			//true way
		}
		
		File root =  new File("c:\\1");
		File child = new File("c:\\1\\1\\1.java");
		assertEquals("1/1.java", Util.getRelativePath(root, child));
		
		//extra slash
		root =  new File("c:\\1\\");
		assertEquals("1/1.java", Util.getRelativePath(root, child));
		
		//path is not absolute
		root =  new File("c:\\1");
		child = new File("1\\1\\1.java");
		assertEquals("1/1/1.java", Util.getRelativePath(root, child));
		
		//path is not absolute
		root =  new File("c:\\1");
		child = new File("..\\1\\1.java");
		assertEquals("../1/1.java", Util.getRelativePath(root, child));
		
	}

	@Test
	public void getFiles_simple() throws Exception {
		assertNotNull(Util.getFiles(MessageFormat.format("{0}{1}{2}{3}", "rootTestFolder", File.separator, "java", File.separator, "1.java")));
	}
	
	@Test
	public void getFiles_all() throws Exception {
		final String pattern = MessageFormat.format("{0}{1}", "rootTestFolder", File.separator);
		final Collection<File> files = Util.getFiles(pattern);
		assertEquals(pattern, 4, files.size());
	}

	
	@Test
	public void getFiles_pattern() throws Exception {
		final String pattern = MessageFormat.format("{0}{1}{2}", "rootTestFolder", File.separator, "**.java");
		final Collection<File> files = Util.getFiles(pattern);
		assertEquals(pattern, 2, files.size());
	}
	
	@Test
	public void getFiles_file_files() throws Exception {
		final File contentFile = new File("@files");
		contentFile.createNewFile();
		try{
			final String content = MessageFormat.format("{0}{1}{2}{3}{4}", "rootTestFolder", File.separator, "java", File.separator, "1.java") + "\n" +
				MessageFormat.format("{0}{1}{2}{3}{4}{5}{6}", "rootTestFolder", File.separator, "java", File.separator, "resources", File.separator, "1.resources");
			
			FileUtil.writeFile(contentFile, content);
			
			final Collection<File> files = Util.getFiles(contentFile);
			assertEquals(2, files.size());
			
		} finally {
			FileUtil.delete(contentFile);
		}
	}
	
	@Test
	public void getFiles_file_folder() throws Exception {
		final File contentFile = new File("@files");
		contentFile.createNewFile();
		try{
			final String content = MessageFormat.format("{0}{1}{2}", "rootTestFolder", File.separator, "java");
			
			FileUtil.writeFile(contentFile, content);
			
			final Collection<File> files = Util.getFiles(contentFile);
			assertEquals(3, files.size());
			
		} finally {
			FileUtil.delete(contentFile);
		}
	}

	@Test
	public void getFiles_file_pattern() throws Exception {
		final File contentFile = new File("@files");
		contentFile.createNewFile();
		try{
			final String content = MessageFormat.format("{0}{1}{2}", "rootTestFolder", File.separator, "**.java");
			
			FileUtil.writeFile(contentFile, content);
			
			final Collection<File> files = Util.getFiles(contentFile);
			assertEquals(2, files.size());
			
		} finally {
			FileUtil.delete(contentFile);
		}
	}
	

}

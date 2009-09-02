package com.jetbrains.teamcity.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;

import jetbrains.buildServer.util.FileUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jetbrains.teamcity.TestServer;
import com.jetbrains.teamcity.resources.IShare;
import com.jetbrains.teamcity.runtime.NullProgressMonitor;

public class RemoteRunTest {
	
	private static RemoteRun ourCommand;
	private static File ourRootFolder;
	private static String ourCurrentDirectory;
	private static TestServer ourTestServer;

	@BeforeClass
	public static void setup() throws Exception {
		//dirs
		ourRootFolder = new File("rootTestFolder");
		ourRootFolder.mkdir();
		final File firsChild = new File(ourRootFolder, "java");
		firsChild.mkdir();
		
		final File firsFirstChild = new File(firsChild, "resources");
		firsFirstChild.mkdir();
		final File secondChild = new File(ourRootFolder, "cpp");
		secondChild.mkdir();
		//files
		new File(firsChild, "1.java").createNewFile();
		new File(firsFirstChild, "2.java").createNewFile();
		new File(firsFirstChild, "1.resources").createNewFile();
		new File(secondChild, "1.cpp").createNewFile();
		
		//keep current directory
		ourCurrentDirectory = System.getProperty("user.dir");
		//change current directory
		System.setProperty("user.dir", ourRootFolder.getCanonicalFile().getAbsolutePath()); 
		
		ourCommand = new RemoteRun();
		
		ourTestServer = new TestServer();
	}
	
	@AfterClass
	public static void clean() throws Exception {
		System.setProperty("user.dir", ourCurrentDirectory);
		FileUtil.delete(ourRootFolder);
	}
	
	@Test
	public void getApplicableConfigurations() {
		//TODO: implement it
	}
	
	@Test
	public void createPatch() {
		//TODO: implement it
	}
	
	@Test
	public void createChangeList() {
		//TODO: implement it
	}
	
	@Test
	public void createInplaceShare() throws Exception {
		try{
			ourCommand.createInplaceShare(null, null);
			assertTrue("Exception expected", true);
		} catch (IllegalArgumentException e){
			//ok
		}
		try{
			ourCommand.createInplaceShare(ourTestServer, TestServer.TC_CONFIG_NO_ROOTS);
			assertTrue("Exception expected", true);
		} catch (IllegalArgumentException e){
			//ok
		}
		try{
			ourCommand.createInplaceShare(ourTestServer, TestServer.TC_CONFIG_MULTIPLE_ROOT);
			assertTrue("Exception expected", true);
		} catch (IllegalArgumentException e){
			//ok
		}
		final IShare singleRootShare = ourCommand.createInplaceShare(ourTestServer, TestServer.TC_CONFIG_SINGLE_ROOT);
		assertNotNull(singleRootShare);
	}
	
	@Test
	public void getRootMap() {
		//TODO: implement it
	}
	
	@Test
	public void fireRemoteRun() {
		//TODO: implement it
	}
		
	@Test
	public void waitForSuccessResult() {
		//TODO: implement it
	}
	
	@Test
	public void getFiles_file_list_passed() {
		//TODO: implement it
	}
	
	@Test
	public void getFiles_file_target_passed() {
		//TODO: implement it		
	}

	@Test
	public void getFiles_nothing_passed() {
		final Collection<File> files = ourCommand.getFiles(new Args(new String[] {RemoteRun.NO_WAIT_SWITCH_LONG, RemoteRun.CONFIGURATION_PARAM_LONG, "bt2"}), new NullProgressMonitor());
		assertNotNull("null file's collection got", files);
		assertEquals("wrong files count collected", 4, files.size());
	}
	
	@Test
	public void validate_error() {
		try{
			ourCommand.validate(null);
			assertTrue("Exception expected", true);
		} catch (IllegalArgumentException e){
			//ok
		}
		try{
			ourCommand.validate(new Args(new String[]{}));
			assertTrue("Exception expected", true);
		} catch (IllegalArgumentException e){
			//ok
		}
		
	}
	
	@Test
	public void validate_ok() {
		ourCommand.validate(new Args(new String[] { ourCommand.ID, ourCommand.MESSAGE_PARAM, "short" }));
		ourCommand.validate(new Args(new String[] { ourCommand.ID, ourCommand.MESSAGE_PARAM_LONG, "long" }));
		ourCommand.validate(new Args(new String[] { ourCommand.ID, ourCommand.MESSAGE_PARAM, "short", 
				ourCommand.MESSAGE_PARAM_LONG, "long" }));
	}
	
	
	
	
}

package com.jetbrains.teamcity.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import jetbrains.buildServer.vcs.VcsRoot;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jetbrains.teamcity.Storage;

public class TCAccessTest {
	
	private static String ourExistingProp;
	
	private static class TestVcsRoot implements VcsRoot{

		public String convertToPresentableString() {
			return null;
		}

		public String convertToString() {
			return null;
		}

		public VcsRoot createSecureCopy() {
			return null;
		}

		public long getId() {
			return 0;
		}

		public String getName() {
			return null;
		}

		public Map<String, String> getProperties() {
			return null;
		}

		public long getPropertiesHash() {
			return 0;
		}

		public String getProperty(String arg0) {
			return null;
		}

		public String getProperty(String arg0, String arg1) {
			return null;
		}

		public long getRootVersion() {
			return 0;
		}

		public String getVcsName() {
			return null;
		}
		
	}

	@BeforeClass
	public static void setup() throws Exception {
		ourExistingProp = System.getProperty(Storage.TC_STORAGE_PROPERTY_NAME);
		System.setProperty(Storage.TC_STORAGE_PROPERTY_NAME, "." + File.separator + ".test.storage");
		
	}
	
	@AfterClass
	public static void teardown() throws Exception {
		if(ourExistingProp != null){
			System.setProperty(Storage.TC_STORAGE_PROPERTY_NAME, ourExistingProp);
		} else {
			System.getProperties().remove(Storage.TC_STORAGE_PROPERTY_NAME);
		}
	}
	
	@Test
	public void share() throws Exception {
		//prepare
		TCAccess.getInstance().clear();
		final String sharePath = "." + File.separator + "abc";
		final File sharedFolder = new File(sharePath);//.getAbsoluteFile();
		sharedFolder.mkdir();
		//test
		try{
			IShare root = TCAccess.getInstance().share(sharePath, new TestVcsRoot());
			assertNotNull(root);
			assertEquals(sharedFolder.getCanonicalPath(), new File(root.getLocal()).getCanonicalPath());
		} finally {
			//clean
			TCAccess.getInstance().clear();
			sharedFolder.delete();
		}
	}
	
	@Test
	public void roots() throws Exception {
		//prepare
		TCAccess.getInstance().clear();
		final String sharePath = "." + File.separator + "abc";
		final File sharedFolder = new File(sharePath);//.getAbsoluteFile();
		sharedFolder.mkdir();
		//test
		try{
			IShare root = TCAccess.getInstance().share(sharePath, new TestVcsRoot());
			assertNotNull(root);
			final Collection<IShare> roots = TCAccess.getInstance().roots();
			assertNotNull(roots);
			assertEquals(1, roots.size());
			for(IShare r : roots){
				assertEquals(root, r);
			}
		} finally {
			//clean
			TCAccess.getInstance().clear();
			sharedFolder.delete();
		}
	}
	
	@Test
	public void unshare() throws Exception {
		//prepare
		TCAccess.getInstance().clear();
		
		final String sharePath1 = "." + File.separator + "abc";
		final File sharedFolder1 = new File(sharePath1);
		sharedFolder1.mkdir();
		
		final String sharePath2 = "." + File.separator + "cde";
		final File sharedFolder2 = new File(sharePath2);
		sharedFolder2.mkdir();
		
		//test
		try{
			final IShare root1 = TCAccess.getInstance().share(sharePath1, new TestVcsRoot());
			TCAccess.getInstance().share(sharePath2, new TestVcsRoot());
			
			TCAccess.getInstance().unshare(root1.getId());
			
			//check removed
			IShare r1 = TCAccess.getInstance().getRoot(sharePath1);
			assertNull(r1);
			//check exists
			IShare r2 = TCAccess.getInstance().getRoot(sharePath2);
			assertNotNull(r2);
			
		} finally {
			//clean
			TCAccess.getInstance().clear();
			sharedFolder2.delete();
			sharedFolder1.delete();
		}
		
	}
	
	@Test
	public void share_inner() throws Exception {
		//prepare
		final TCAccess access = TCAccess.getInstance();
		access.clear();
		
		final String sharePath1 = "." + File.separator + "abc";
		final File sharedFolder1 = new File(sharePath1);//.getAbsoluteFile();
		sharedFolder1.mkdir();
		final File sharedFile1 = new File(sharedFolder1, "shared.1");
		sharedFile1.createNewFile();
		
		final String sharePath2 = "." + File.separator + "abc" + File.separator + "cde";
		final File sharedFolder2 = new File(sharePath2);//.getAbsoluteFile();
		sharedFolder2.mkdir();
		final File sharedFile2 = new File(sharedFolder2, "shared.2");
		sharedFile2.createNewFile();
		
		//test
		try{
			final IShare root1 = access.share(sharePath1, new TestVcsRoot());
			final IShare root2 = access.share(sharePath2, new TestVcsRoot());
			
			final IShare foundRoot1 = access.getRoot(sharedFile1.getPath());
			assertNotNull(foundRoot1);
			assertEquals(root1, foundRoot1);
			
			final IShare foundRoot2 = access.getRoot(sharedFile2.getPath());
			assertNotNull(foundRoot2);
			assertEquals(root2, foundRoot2);
			
		} finally {
			//clean
			access.clear();
			sharedFile1.delete();
			sharedFile2.delete();
			sharedFolder2.delete();
			sharedFolder1.delete();
		}
		
	}
	
	@Test
	public void getRoot() throws Exception {
		//prepare
		TCAccess.getInstance().clear();
		final String sharePath = "." + File.separator + "abc";
		final File sharedFolder = new File(sharePath);//.getAbsoluteFile();
		sharedFolder.mkdir();
		//test
		try{
			TCAccess.getInstance().share(sharePath, new TestVcsRoot());
			IShare found = TCAccess.getInstance().getRoot(".\\abc");
			assertNotNull(found);
			found = TCAccess.getInstance().getRoot(".\\abc\\");
			assertNotNull(found);
			found = TCAccess.getInstance().getRoot("./abc");
			assertNotNull(found);
			found = TCAccess.getInstance().getRoot("./abc/");
			assertNotNull(found);
			found = TCAccess.getInstance().getRoot("./AbC ");
			assertNotNull(found);
			found = TCAccess.getInstance().getRoot("./aBc/ ");
			assertNotNull(found);
			found = TCAccess.getInstance().getRoot("/aBc/ ");
			assertNull(found);
			found = TCAccess.getInstance().getRoot("./cde/");
			assertNull(found);
			found = TCAccess.getInstance().getRoot("./aBc/Def\\J.k ");
			assertNotNull(found);
		} finally {
			//clean
			TCAccess.getInstance().clear();
			sharedFolder.delete();
		}
	}
	

	
}

package com.jetbrains.teamcity.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jetbrains.teamcity.Storage;

public class VCSAccessTest {
	
	private static String ourExistingProp;

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
		VCSAccess.getInstance().clear();
		final String sharePath = "." + File.separator + "abc";
		final File sharedFolder = new File(sharePath);//.getAbsoluteFile();
		sharedFolder.mkdir();
		//test
		try{
			IVCSRoot root = VCSAccess.getInstance().share(sharePath, -1l);
			assertNotNull(root);
			assertEquals(sharedFolder.getCanonicalPath(), new File(root.getLocal()).getCanonicalPath());
		} finally {
			//clean
			VCSAccess.getInstance().clear();
			sharedFolder.delete();
		}
	}
	
	@Test
	public void roots() throws Exception {
		//prepare
		VCSAccess.getInstance().clear();
		final String sharePath = "." + File.separator + "abc";
		final File sharedFolder = new File(sharePath);//.getAbsoluteFile();
		sharedFolder.mkdir();
		//test
		try{
			IVCSRoot root = VCSAccess.getInstance().share(sharePath, -1l);
			assertNotNull(root);
			final Collection<IVCSRoot> roots = VCSAccess.getInstance().roots();
			assertNotNull(roots);
			assertEquals(1, roots.size());
			for(IVCSRoot r : roots){
				assertEquals(root, r);
			}
		} finally {
			//clean
			VCSAccess.getInstance().clear();
			sharedFolder.delete();
		}
	}
	
	@Test
	public void unshare() throws Exception {
		//prepare
		VCSAccess.getInstance().clear();
		
		final String sharePath1 = "." + File.separator + "abc";
		final File sharedFolder1 = new File(sharePath1);
		sharedFolder1.mkdir();
		
		final String sharePath2 = "." + File.separator + "cde";
		final File sharedFolder2 = new File(sharePath2);
		sharedFolder2.mkdir();
		
		//test
		try{
			final IVCSRoot root1 = VCSAccess.getInstance().share(sharePath1, -1l);
			VCSAccess.getInstance().share(sharePath2, -2l);
			
			VCSAccess.getInstance().unshare(root1.getId());
			
			//check removed
			IVCSRoot r1 = VCSAccess.getInstance().getRoot(sharePath1);
			assertNull(r1);
			//check exists
			IVCSRoot r2 = VCSAccess.getInstance().getRoot(sharePath2);
			assertNotNull(r2);
			
		} finally {
			//clean
			VCSAccess.getInstance().clear();
			sharedFolder2.delete();
			sharedFolder1.delete();
		}
		
	}
	
	/**
	 * do not allow share inner  
	 */
	@Test(expected=IllegalArgumentException.class)
	public void wrong_share_inner() throws Exception {
		//prepare
		VCSAccess.getInstance().clear();
		
		final String sharePath1 = "." + File.separator + "abc";
		final File sharedFolder1 = new File(sharePath1);//.getAbsoluteFile();
		sharedFolder1.mkdir();
		
		final String sharePath2 = "." + File.separator + "abc" + File.separator + "cde";
		final File sharedFolder2 = new File(sharePath2);//.getAbsoluteFile();
		sharedFolder2.mkdir();
		
		//test
		try{
			VCSAccess.getInstance().share(sharePath1, -1l);
			VCSAccess.getInstance().share(sharePath2, -2l);
		} finally {
			//clean
			VCSAccess.getInstance().clear();
			sharedFolder2.delete();
			sharedFolder1.delete();
		}
		
	}
	
	/**
	 * do not allow share inner  
	 */
	@Test(expected=IllegalArgumentException.class)
	public void wrong_share_outer() throws Exception {
		//prepare
		VCSAccess.getInstance().clear();
		
		final String sharePath1 = "." + File.separator + "abc";
		final File sharedFolder1 = new File(sharePath1);//.getAbsoluteFile();
		sharedFolder1.mkdir();
		
		final String sharePath2 = "." + File.separator + "abc" + File.separator + "cde";
		final File sharedFolder2 = new File(sharePath2);//.getAbsoluteFile();
		sharedFolder2.mkdir();
		
		//test
		try{
			VCSAccess.getInstance().share(sharePath2, -2l);
			VCSAccess.getInstance().share(sharePath1, -1l);
		} finally {
			//clean
			VCSAccess.getInstance().clear();
			sharedFolder2.delete();
			sharedFolder1.delete();
		}
		
	}
	
	@Test
	public void getRoot() throws Exception {
		//prepare
		VCSAccess.getInstance().clear();
		final String sharePath = "." + File.separator + "abc";
		final File sharedFolder = new File(sharePath);//.getAbsoluteFile();
		sharedFolder.mkdir();
		//test
		try{
			VCSAccess.getInstance().share(sharePath, -1l);
			IVCSRoot found = VCSAccess.getInstance().getRoot(".\\abc");
			assertNotNull(found);
			found = VCSAccess.getInstance().getRoot(".\\abc\\");
			assertNotNull(found);
			found = VCSAccess.getInstance().getRoot("./abc");
			assertNotNull(found);
			found = VCSAccess.getInstance().getRoot("./abc/");
			assertNotNull(found);
			found = VCSAccess.getInstance().getRoot("./AbC ");
			assertNotNull(found);
			found = VCSAccess.getInstance().getRoot("./aBc/ ");
			assertNotNull(found);
			found = VCSAccess.getInstance().getRoot("/aBc/ ");
			assertNull(found);
			found = VCSAccess.getInstance().getRoot("./cde/");
			assertNull(found);
			found = VCSAccess.getInstance().getRoot("./aBc/Def\\J.k ");
			assertNotNull(found);
		} finally {
			//clean
			VCSAccess.getInstance().clear();
			sharedFolder.delete();
		}
	}
	

	
}

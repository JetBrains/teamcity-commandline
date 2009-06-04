package com.jetbrains.teamcity;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jetbrains.teamcity.Storage.IKey;

public class SorageTest {
	
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
	public void put() throws Exception {
		IKey<Long> testKey = new Storage.IKey<Long>(){
			private static final long serialVersionUID = 6509717225043443905L;
			public Object getKey() {
				return "TestKey";
			}
		};
		Storage.getInstance().put(testKey, new Long(12345));
		//discard caching
		Storage.reload();
		//check
		assertEquals(new Long(12345), Storage.getInstance().get(testKey));
	}
	
	@Test
	public void put_owerride() throws Exception {
		IKey<Long> testKey = new Storage.IKey<Long>(){
			private static final long serialVersionUID = 6509717225043443905L;
			public Object getKey() {
				return "TestKey";
			}
		};
		Storage.getInstance().put(testKey, new Long(12345));
		Storage.getInstance().put(testKey, new Long(67890));
		//discard caching
		Storage.reload();
		//check
		assertEquals(new Long(67890), Storage.getInstance().get(testKey));
	}
	
	@Test
	public void put_together() throws Exception {
		IKey<Long> testKey1 = new Storage.IKey<Long>(){
			
			private static final long serialVersionUID = 1L;

			public Object getKey() {
				return "TestKey1";
			}
		};
		
		IKey<Long> testKey2 = new Storage.IKey<Long>(){
			
			private static final long serialVersionUID = 1L;
			
			public Object getKey() {
				return "TestKey2";
			}
		};
		
		Storage.getInstance().put(testKey1, new Long(12345));
		Storage.getInstance().put(testKey2, new Long(67890));
		//discard caching
		Storage.reload();
		
		//check
		assertEquals(new Long(12345), Storage.getInstance().get(testKey1));
		assertEquals(new Long(67890), Storage.getInstance().get(testKey2));
	}
	
	

}

package com.jetbrains.teamcity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

public class Storage {

	public static final String TC_STORAGE_PROPERTY_NAME = "tc.storage";

	private static Storage ourInstance;

	@SuppressWarnings("unchecked")
	private HashMap<Object, Serializable> myStorage = new HashMap<Object, Serializable>();

	private String myStorageFile;

	@SuppressWarnings("unchecked")
	private Storage() {
		//init storage
		final String storageFile = System.getProperty(TC_STORAGE_PROPERTY_NAME);
		if(storageFile != null){
			myStorageFile = storageFile;
		} else {//set to default
			final String home = System.getProperty("user.home");
			myStorageFile = home + File.separator + ".tc.storage";
		}
		//load storage
		try {
			final ObjectInputStream in = new ObjectInputStream(new FileInputStream(getStorageFile()));
			myStorage = (HashMap<Object, Serializable>) in.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			// do nothing
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private String getStorageFile() {
		// TODO Auto-generated method stub
		return myStorageFile;
	}

	public synchronized static Storage getInstance() {
		if (ourInstance == null) {
			ourInstance = new Storage();
		}
		return ourInstance;
	}
	
	synchronized static void reload() {
		ourInstance = new Storage();
	}

	@SuppressWarnings("unchecked")
	public synchronized <T extends Serializable> T get(final IKey<T> key) {
		return (T) myStorage.get(key.getKey());
	}

	public synchronized <T extends Serializable> void put(final IKey<T> key, T value, final boolean flush) {
		myStorage.put(key.getKey(), value);
		if(flush){
			try {
				final ObjectOutput out = new ObjectOutputStream(new FileOutputStream(getStorageFile()));
				out.writeObject(myStorage);
				out.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public synchronized <T extends Serializable> void put(final IKey<T> key, T value) {
		put(key, value, true);
	}
	

	public static interface IKey<T extends Serializable> extends Serializable {
		Object getKey();
	}

}

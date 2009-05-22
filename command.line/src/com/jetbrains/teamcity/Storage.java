package com.jetbrains.teamcity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.messages.XStreamHolder;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.xstream.ServerXStreamFormat;
import jetbrains.buildServer.xstream.XStreamWrapper;

import com.thoughtworks.xstream.XStream;

//TODO: support parallel working!!!
public class Storage {

	public static final String TC_STORAGE_PROPERTY_NAME = "tc.storage";
	public static final String TC_STORAGE_DEFAULT_FILENAME = ".tcstorage";

	private static Storage ourInstance;
	
	private static IStorageFS ourStorageFS; 

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
			myStorageFile = home + File.separator + TC_STORAGE_DEFAULT_FILENAME;
		}
		//create FS
//		ourStorageFS = new JavaStorageFS(myStorageFile);
		ourStorageFS = new XMLStorageFS(myStorageFile);
		//load storage
		ourStorageFS.load(myStorage);
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
			ourStorageFS.save(myStorage);
		}
	}

	public synchronized <T extends Serializable> void put(final IKey<T> key, T value) {
		put(key, value, true);
	}
	

	public static interface IKey<T extends Serializable> extends Serializable {
		Object getKey();
	}
	
	private interface IStorageFS {
		void save(final HashMap<Object,Serializable> storage);
		Map<Object,Serializable> load(final HashMap<Object,Serializable> storage);
	}
	
	private static class JavaStorageFS implements IStorageFS{

		public JavaStorageFS(String myStorageFile) {
			this.myStorageFile = myStorageFile;
		}

		private String myStorageFile;

		@Override
		public Map<Object, Serializable> load(HashMap<Object, Serializable> storage) {
			try {
				storage.clear();
				final ObjectInputStream in = new ObjectInputStream(new FileInputStream(getStorageFile()));
				storage.putAll((HashMap<Object, Serializable>) in.readObject());
			} catch (FileNotFoundException e) {
				// do nothing
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return storage;
		}

		@Override
		public void save(HashMap<Object, Serializable> storage) {
			try {
				final ObjectOutput out = new ObjectOutputStream(new FileOutputStream(getStorageFile()));
				out.writeObject(storage);
				out.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		private String getStorageFile() {
			return this.myStorageFile;
		}
		
	}
	
	private static class XMLStorageFS implements IStorageFS{
		
		private String myStorageFile;
		
		public XMLStorageFS(final String myStorageFile) {
			this.myStorageFile = myStorageFile + ".xml";
		}
		
		//TODO: move to utils		
		private <T> T deserializeObject(final Object typeData) {
			return XStreamWrapper.<T>deserializeObject((String)typeData, ourXStreamHolder);
		}
		
		//TODO: move to utils		
		private String serializeObject(final Object typeData) {
			return XStreamWrapper.serializeObject(typeData, ourXStreamHolder);
		}
		
		//TODO: move to utils		
		private static XStreamHolder ourXStreamHolder = new XStreamHolder() {
			protected void configureXStream(XStream xStream) {
				ServerXStreamFormat.formatXStream(xStream);
			}
		};

		@Override
		public Map<Object, Serializable> load(HashMap<Object, Serializable> storage) {
			try {
				storage.clear();
				final String content = FileUtil.loadTextAndClose(new FileReader(getStorageFile()));
				storage.putAll((Map<? extends Object, ? extends Serializable>) deserializeObject(content));
			} catch (FileNotFoundException e) {
				// do nothing
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return storage;
		}

		@Override
		public void save(HashMap<Object, Serializable> storage) {
			final String xml = serializeObject(storage);
			FileUtil.writeFile(new File(getStorageFile()), xml);
		}
		
		private String getStorageFile() {
			return this.myStorageFile;
		}
		
	}

}

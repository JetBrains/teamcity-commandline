
package com.jetbrains.teamcity;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.messages.XStreamHolder;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.xstream.ServerXStreamFormat;
import jetbrains.buildServer.xstream.XStreamWrapper;

//TODO: support parallel working!!!
public class Storage {

  public static final String TC_STORAGE_PROPERTY_NAME = "teamcity.cli.configfile"; //$NON-NLS-1$
  public static final String TC_STORAGE_ENVIRONMENT_VARIABLE_PROPERTY_NAME = "TEAMCITY_CLI_CONFIGFILE"; //$NON-NLS-1$
  public static final String TC_CLI_HOME = "/.TeamCity-CommandLine"; //$NON-NLS-1$
  public static final String TC_STORAGE_DEFAULT_FILENAME = TC_CLI_HOME + "/.tcstorage"; //$NON-NLS-1$

  private static Storage ourInstance;

  private static IStorageFS ourStorageFS;

  private final HashMap<Object, Serializable> myStorage = new HashMap<Object, Serializable>();

  private final String myStorageFile;

  private Storage() {
    // init storage
    final String storageFile = System.getProperty(TC_STORAGE_PROPERTY_NAME);
    if (storageFile != null) {// -D
      myStorageFile = storageFile;

    } else if (System.getenv(TC_STORAGE_ENVIRONMENT_VARIABLE_PROPERTY_NAME) != null) {// env
      myStorageFile = System.getenv(TC_STORAGE_ENVIRONMENT_VARIABLE_PROPERTY_NAME);

    } else {// default
      final String home = System.getProperty("user.home"); //$NON-NLS-1$
      myStorageFile = home + File.separator + TC_STORAGE_DEFAULT_FILENAME;
    }

    ourStorageFS = new XMLStorageFS(myStorageFile);
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
    if (flush) {
      flush();
    }
  }

  public synchronized void flush() {
    ourStorageFS.save(myStorage);
  }

  public synchronized <T extends Serializable> void put(final IKey<T> key, T value) {
    put(key, value, true);
  }

  public static interface IKey<T extends Serializable> extends Serializable {
    Object getKey();
  }

  private interface IStorageFS {
    void save(final HashMap<Object, Serializable> storage);

    Map<Object, Serializable> load(final HashMap<Object, Serializable> storage);
  }

  private static class XMLStorageFS implements IStorageFS {

    private final String myStorageFile;

    public XMLStorageFS(final String myStorageFile) {
      this.myStorageFile = myStorageFile + ".xml"; //$NON-NLS-1$
    }

    // TODO: move to utils
    private <T> T deserializeObject(final Object typeData) {
      return XStreamWrapper.<T> deserializeObject((String) typeData, ourXStreamHolder);
    }

    // TODO: move to utils
    private String serializeObject(final Object typeData) {
      return XStreamWrapper.serializeObject(typeData, ourXStreamHolder);
    }

    // TODO: move to utils
    private static final XStreamHolder ourXStreamHolder = new XStreamHolder() {
      protected void configureXStream(XStream xStream) {
        ServerXStreamFormat.formatXStream(xStream);
      }
    };

    public Map<Object, Serializable> load(HashMap<Object, Serializable> storage) {
      try {
        storage.clear();
        final String content = FileUtil.readText(new File(getStorageFile()));
        storage.putAll((Map<?, ? extends Serializable>) deserializeObject(content));
      } catch (FileNotFoundException|NoSuchFileException e) {
        // do nothing
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return storage;
    }

    public void save(HashMap<Object, Serializable> storage) {
      final String xml = serializeObject(storage);
      FileUtil.writeFile(new File(getStorageFile()), xml);
    }

    private String getStorageFile() {
      return this.myStorageFile;
    }

  }

}
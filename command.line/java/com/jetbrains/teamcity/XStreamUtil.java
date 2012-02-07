package com.jetbrains.teamcity;

import com.thoughtworks.xstream.XStream;
import jetbrains.buildServer.messages.XStreamHolder;
import jetbrains.buildServer.xstream.ServerXStreamFormat;
import jetbrains.buildServer.xstream.XStreamWrapper;

import java.io.IOException;
import java.util.Vector;

public class XStreamUtil {
  private final static XStreamHolder ourXStreamHolder = new XStreamHolder() {
    protected void configureXStream(XStream xStream) {
      ServerXStreamFormat.formatXStream(xStream);
    }
  };

  public static <T> T deserializeObject(final Object typeData) {
    return XStreamWrapper.<T>deserializeObject((String) typeData, ourXStreamHolder);
  }

  public static <T> Vector serializeObjects(final java.util.List<T> list) {
    return XStreamWrapper.<T>serializeObjects(list, ourXStreamHolder);
  }

  public static <T> T unzipAndDeserializeObject(final Object typeData) throws IOException {
    return XStreamWrapper.<T>unzipAndDeserializeObject((byte[]) typeData, ourXStreamHolder);
  }
}

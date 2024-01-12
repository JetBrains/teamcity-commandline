
package com.jetbrains.teamcity.resources;

import java.io.File;

public interface ITCResourceMatcher {

  public Matching getMatching(final File file) throws IllegalArgumentException;

  public interface Matching {

    public String getTCID();

    public String getRelativePath();

  }

}
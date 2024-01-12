
package com.jetbrains.teamcity.resources;

import com.jetbrains.teamcity.Util;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;

public class FileBasedMatcher implements ITCResourceMatcher {

  private static final String CURRENT_DIR = ".";

  private static final String FIELD_DEVIDER = "=";

  private static final Comparator<String> PATH_SORTER = new Comparator<String>() {
    public int compare(final String key0, final String key1) {
      return key1.toLowerCase().compareTo(key0.toLowerCase());
    }
  };

  private final File myFile;
  private final TreeMap<String, String> myRulesMap = new TreeMap<String, String>(PATH_SORTER);

  public static FileBasedMatcher create(final File rootFolder, final Map<File, String> localToRepo) throws IllegalArgumentException {
    // check arguments
    if (rootFolder == null || localToRepo == null) {
      throw new IllegalArgumentException(MessageFormat.format("Root or Map is null: {0}, {1}", rootFolder, localToRepo));
    }
    // create content
    try {
      final File absoluteRoot = rootFolder.getCanonicalFile().getAbsoluteFile();
      final HashMap<String, String> pathToRepoMap = new HashMap<String, String>();

      // transform into plain Strings
      for (final Map.Entry<File, String> entry : localToRepo.entrySet()) {

        final File absoluteEntryFile = entry.getKey().getCanonicalFile();
        final String repoPrefix = Util.toPortableString(entry.getValue());

        if (absoluteRoot.equals(absoluteEntryFile)) {
          // provide "."
          pathToRepoMap.put(CURRENT_DIR, repoPrefix);
        } else {
          final String relativePath = FileUtil.getRelativePath(absoluteRoot, absoluteEntryFile);
          pathToRepoMap.put(relativePath, repoPrefix);
        }
      }
      // persist
      final StringBuffer buffer = new StringBuffer();
      for (Map.Entry<String, String> entry : pathToRepoMap.entrySet()) {
        buffer.append(entry.getKey()).append(FIELD_DEVIDER).append(entry.getValue()).append("\n");
      }
      final File adminFile = new File(rootFolder, TCWorkspace.TCC_ADMIN_FILE);
      FileUtil.writeFile(adminFile, buffer.toString().trim());

      return new FileBasedMatcher(adminFile);

    } catch (IOException e) {
      throw new IllegalArgumentException(e);

    }
  }

  public FileBasedMatcher(final File file) {
    if (file == null || !file.exists()) {
      throw new IllegalArgumentException(MessageFormat.format("File is null or not extists: \"{0}\"", file));
    }
    try {
      myFile = file.getAbsoluteFile();
      // parse content
      final List<String> items = FileUtil.readFile(myFile);
      if (items.isEmpty()) {
        throw new IllegalArgumentException(MessageFormat.format("\"{0}\" is empty", myFile));
      }
      // will build ordered by full path map for next local files mapping
      for (final String item : items) {
        final String[] columns = item.trim().split(FIELD_DEVIDER);
        if (columns.length < 2) {
          throw new IllegalArgumentException(MessageFormat.format("\"{0}\" format is wrong", myFile));
        }

        final String path = columns[0];
        final String tcid = StringUtil.removeTailingSlash(columns[1]);// slash
                                                                      // will be
                                                                      // added
                                                                      // to Url
                                                                      // later
        // absolute or not: if relative have to add the source file location to
        // paths describes mappings from the file
        File ruleContainer = new File(path);
        if (!ruleContainer.isAbsolute()) {
          ruleContainer = new File(myFile.getParentFile().getAbsoluteFile(), path);
        }
        myRulesMap.put(Util.toPortableString(ruleContainer.getCanonicalFile().getAbsolutePath()), tcid);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public Matching getMatching(final File file) throws IllegalArgumentException {
    try {
      final String filePath = Util.toPortableString(file.getCanonicalFile().getAbsolutePath());
      for (final String rulePath : myRulesMap.keySet()) {
        if (filePath.startsWith(rulePath)) {

          String relativePath = FileUtil.getRelativePath(rulePath, filePath, '/');
          if (".".equals(relativePath)) relativePath = "";
          if (relativePath == null || relativePath.startsWith("..")) continue;
          
          return new MatchingImpl(myRulesMap.get(rulePath), relativePath);
        }
      }
      return null;

    } catch (IOException e) {
      throw new IllegalArgumentException(e);

    }
  }

  static class MatchingImpl implements ITCResourceMatcher.Matching {
    private final String myPrefix;
    private final String myRelativePath;

    MatchingImpl(final String prefix, final String relativePath) {
      myPrefix = prefix;
      myRelativePath = relativePath;
    }

    public String getTCID() {
      return myPrefix;
    }

    public String getRelativePath() {
      return myRelativePath;
    }

  }

  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("File: ").append(myFile.getAbsolutePath()).append(":\n");
    for (Map.Entry<String, String> entry : myRulesMap.entrySet()) {
      buffer.append(entry.getKey()).append(FIELD_DEVIDER).append(entry.getValue()).append("\n");
    }
    return buffer.toString().trim();
  }

}
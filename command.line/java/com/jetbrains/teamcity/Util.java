/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.teamcity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;

public class Util {

  public static IFileFilter CVS_FILES_FILTER = new CVSFilter();

  public static IFileFilter SVN_FILES_FILTER = new SVNFilter();

  private static Pattern ASTERISK_PATTERN = Pattern.compile(".*"); //$NON-NLS-1$

  public static String getArgumentValue(final String[] args, final String... arguments) {
    for (int i = 0; i < args.length; i++) {
      for (String argument : arguments) {
        if (argument != null) {
          if (args[i].toLowerCase().trim().equals(argument.toLowerCase().trim()) && args.length > (i + 1)) {
            return args[i + 1].trim();
          }
        }
      }
    }
    return null;
  }

  public static boolean hasArgument(final String[] args, final String... arguments) {
    for (int i = 0; i < args.length; i++) {
      for (String argument : arguments) {
        if (argument != null) {
          if (args[i].toLowerCase().trim().equals(argument.toLowerCase().trim())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static List<String> getPathList(File f) {
    List<String> l = new ArrayList<String>();
    File r;
    try {
      r = f.getCanonicalFile();
      while (r != null) {
        l.add(r.getName());
        r = r.getParentFile();
      }
    } catch (IOException e) {
      l = null;
      Debug.getInstance().error(Util.class, e.getMessage(), e);
    }
    return l;
  }

  private static String matchPathLists(List<String> r, List<String> f) {
    int i;
    int j;
    String s;
    // start at the beginning of the lists
    // iterate while both lists are equal
    s = "";
    i = r.size() - 1;
    j = f.size() - 1;

    // first eliminate common root
    while ((i >= 0) && (j >= 0) && (r.get(i).equals(f.get(j)))) {
      i--;
      j--;
    }

    // for each remaining level in the home path, add a ..
    for (; i >= 0; i--) {
      s += ".." + File.separator;
    }

    // for each level in the file path, add the path
    for (; j >= 1; j--) {
      s += f.get(j) + File.separator;
    }

    // file name
    s += f.get(j);
    return s;
  }

  public static String getRelativePathImpl(File home, File f) {
    final List<String> homelist = getPathList(home);
    final List<String> filelist = getPathList(f);
    final String s = matchPathLists(homelist, filelist);
    return s;
  }

  public static String getRelativePath(final File root, final File to) throws IOException, IllegalArgumentException {
    if (root == null || to == null) {
      throw new IllegalArgumentException(MessageFormat.format("Null is not supported as argument: {0}, {1}", root, to)); //$NON-NLS-1$
    }
    if (to.isAbsolute()) {
      String relativePath = getRelativePathImpl(root, to);
      return Util.toPortableString(relativePath); //$NON-NLS-1$ //$NON-NLS-2$
    } else {
      return Util.toPortableString(to.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  public static Collection<File> getFiles(final String path) throws IllegalArgumentException {
    try {
      final File simpleFile = new File(path).getCanonicalFile().getAbsoluteFile();
      if (simpleFile.exists() && simpleFile.isFile()) {
        return Collections.<File> singletonList(simpleFile);
      } else if (simpleFile.exists() && simpleFile.isDirectory()) {
        final ArrayList<File> list = new ArrayList<File>();
        FileUtil.collectMatchedFiles(simpleFile, ASTERISK_PATTERN, list);
        return list;
      } else if (hasFilePatterns(path)) {
        final ArrayList<File> list = new ArrayList<File>();
        FileUtil.collectMatchedFiles(simpleFile, Pattern.compile(path), list);
        return list;
      }
      return Collections.singletonList(simpleFile);// let it be
    } catch (IOException e) {
      throw new IllegalArgumentException(MessageFormat.format("Wrong path passed: {0}", path));
    }
  }

  static boolean hasFilePatterns(String path) {
    return path.contains("*") || path.contains("!"); //$NON-NLS-1$//$NON-NLS-2$
  }

  public static Collection<File> getFiles(final File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException(MessageFormat.format("File is not found \"{0}\"", file.getAbsolutePath())); //$NON-NLS-1$
    }
    if (file.length() == 0) {
      throw new IllegalArgumentException(MessageFormat.format("File \"{0}\" is empty", file.getAbsolutePath())); //$NON-NLS-1$	
    }
    try {
      final List<String> content = FileUtil.readFile(file);
      final HashSet<File> files = new HashSet<File>(content.size());
      for (String path : content) {
        if (path.trim().length() > 0) {
          files.addAll(getFiles(path));
        }
      }
      return files;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public interface IFileFilter {
    public Collection<File> accept(final Collection<File> files);
  }

  private static class CVSFilter implements IFileFilter {

    public Collection<File> accept(Collection<File> files) {
      final HashSet<File> result = new HashSet<File>();
      for (final File file : files) {
        final String normalPath = Util.toPortableString(file.getPath().toLowerCase());
        if (!normalPath.endsWith("cvs/entries") && !normalPath.endsWith("cvs/repository") && !normalPath.endsWith("cvs/root")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          result.add(file);
        }
      }
      return result;
    }
  }

  private static class SVNFilter implements IFileFilter {

    public Collection<File> accept(Collection<File> files) {
      final HashSet<File> result = new HashSet<File>();
      for (final File file : files) {
        final String normalPath = file.getPath().toLowerCase();
        if (!normalPath.contains(".svn")) { //$NON-NLS-1$
          result.add(file);
        }
      }
      return result;
    }
  }

  public static Throwable getRootCause(Throwable throwable) {
    Throwable cause = throwable.getCause();
    if (cause != null) {
      throwable = cause;
      while ((throwable = throwable.getCause()) != null) {
        cause = throwable;
      }
      return cause;
    }
    return throwable;
  }

  public static class StringTable {

    private String[] myHeader;
    private LinkedList<String[]> rows = new LinkedList<String[]>();
    private int myNumColumns;

    public StringTable(final String[] header) {
      if (header == null || header.length == 0) {
        throw new IllegalArgumentException("Header cannot be null or empty");
      }
      myHeader = header;
      myNumColumns = myHeader.length;
    }

    public StringTable(final int numColumns) {
      myNumColumns = numColumns;
    }

    /**
     * @param tabbedHeader
     *          Plain string represents Column's headers devided by '\t'
     */
    public StringTable(final String tabbedHeader) {
      this(tabbedHeader.split("\t"));
    }

    /**
     * 
     * @param tabbedRow
     *          Plain string represents row. columns must be devided by '\t'
     */
    public void addRow(final String tabbedRow) {
      final String[] splited = tabbedRow.split("\t");
      if (splited.length == myNumColumns) {
        addRow(splited);

      } else if (splited.length < myNumColumns) {
        final String[] paddedRow = new String[myNumColumns];
        Arrays.fill(paddedRow, "");
        for (int i = 0; i < splited.length; i++) {
          paddedRow[i] = splited[i];
        }
        addRow(paddedRow);
      } else {
        final String[] trimmedRow = new String[myNumColumns];
        for (int i = 0; i < myNumColumns; i++) {
          trimmedRow[i] = splited[i];
        }
        addRow(trimmedRow);
      }
    }

    public void addRow(final String[] row) {
      if (row == null || row.length != myNumColumns) {
        throw new IllegalArgumentException(MessageFormat.format("Row is null or size differs to Header {1}: {0}", Arrays.toString(row), myNumColumns));
      }
      rows.add(row);
    }

    public String toString() {
      final LinkedList<String[]> buffer = new LinkedList<String[]>(rows);
      if (myHeader != null) {
        buffer.add(0, myHeader);
      }

      // collect max lengths of columns
      final int[] maxSizes = new int[myNumColumns];
      for (final String[] row : buffer) {
        for (int i = 0; i < myNumColumns; i++) {
          if (row[i] != null) {
            maxSizes[i] = Math.max(row[i].length(), maxSizes[i]);
          }
        }
      }
      // so, let's format result according to maxSizes...
      final StringBuffer result = new StringBuffer();
      for (final String[] row : buffer) {
        for (int i = 0; i < myNumColumns; i++) {
          final String column = row[i] != null ? row[i].replace("\n", "\\") : "";
          final int maxStringLenght = maxSizes[i];
          result.append(String.format("%1$-" + ((i != (myNumColumns - 1)) ? (maxStringLenght + 1) : maxStringLenght) + "s", column));

        }
        result.append("\n");
      }
      return result.toString();
    }

  }

  public static String readConsoleInput(String prompt, boolean secure) {
    final byte minorVersion = getJavaVersion()[1];
    if (!secure || minorVersion < 6) {
      if (prompt != null) {
        System.out.print(prompt);
      }
      final Scanner scanner = new Scanner(System.in);
      String line = scanner.nextLine();
      return line;

    } else {
      return readPassword(prompt);

    }
  }

  /**
   * have to use reflection because of 1.5 target platform
   */
  private static String readPassword(final String prompt) {
    try {
      final Method method = System.class.getDeclaredMethod("console");
      if (method != null) {
        final Object consoleObj = method.invoke(null);
        if (consoleObj != null) {
          final Method passwdMthd = consoleObj.getClass().getMethod("readPassword", String.class, new Object[0].getClass());
          if (passwdMthd != null) {
            final char[] passwd = (char[]) passwdMthd.invoke(consoleObj, "%s", new Object[] { prompt });
            return new String(passwd);
          }
        }
      }
    } catch (Throwable e) {
      // do nothing
    }
    return readConsoleInput(prompt, false);// read anyway
  }

  public static byte[] getJavaVersion() {
    byte[] out = new byte[2];
    final String[] tokens = System.getProperty("java.version", "1.5").split("\\.");
    out[0] = Byte.parseByte(tokens[0]);
    out[1] = Byte.parseByte(tokens[1]);
    return out;
  }

  public static String toPortableString(final String path) {
    if (path != null) {
      return StringUtil.removeTailingSlash(path.replace("\\", "/"));
    }
    return null;
  }

  public static File getCurrentDirectory() {
    return new File(System.getProperty("user.dir"));
  }

  public static String encode(String src) {
    boolean decoded = false;
    int length = src.length();
    ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
    for (int i = 0; i < length; i++) {
      byte ch = (byte) src.charAt(i);
      if (ch == '%' && i + 2 < length && isHexDigit(src.charAt(i + 1)) && isHexDigit(src.charAt(i + 2))) {
        ch = (byte) (hexValue(src.charAt(i + 1)) * 0x10 + hexValue(src.charAt(i + 2)));
        decoded = true;
        i += 2;
      }
      bos.write(ch);
    }
    if (!decoded) {
      return src;
    }
    try {
      return new String(bos.toByteArray(), "UTF-8"); //$NON-NLS-1$
    } catch (UnsupportedEncodingException e) {
    }
    return src;
  }

  private static boolean isHexDigit(char ch) {
    return Character.isDigit(ch) || (Character.toUpperCase(ch) >= 'A' && Character.toUpperCase(ch) <= 'F');
  }

  private static int hexValue(char ch) {
    if (Character.isDigit(ch)) {
      return ch - '0';
    }
    ch = Character.toUpperCase(ch);
    return (ch - 'A') + 0x0A;
  }

  public static String trim(String string, final String... tokens) {
    if (string != null) {
      string = string.trim();
      if (tokens.length > 0) {
        for (String token : tokens) {
          if (string.endsWith(token)) {
            string = string.substring(0, string.length() - token.length());
            string = trim(string, tokens);
          }
        }
      }
    }
    return string;
  }

  public static <T> Collection<T> intersect(Collection<T> first, Collection<T> second) {
    HashSet<T> out = new HashSet<T>();
    for (T element : first) {
      if (second.contains(element)) {
        out.add(element);
      }
    }
    return out;
  }

}

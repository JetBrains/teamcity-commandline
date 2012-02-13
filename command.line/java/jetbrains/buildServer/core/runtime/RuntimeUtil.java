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
package jetbrains.buildServer.core.runtime;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

public class RuntimeUtil {

  public static void execAndWait(final String command, final IProgressMonitor monitor) throws IOException {
    execAndWait(command, new File("."), monitor);
  }

  public static Process exec(final String[] commands, final IProgressMonitor monitor) throws IOException {
    return exec(commands, new File("."), monitor);
  }

  public static Process exec(final String[] commands, final File dir, final IProgressMonitor monitor) throws IOException {
    monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("Executing command: \"%s\" in %s", Arrays.toString(commands), dir)));
//    final String[] envp = makeEnv(System.getenv());
    final Process process = Runtime.getRuntime().exec(commands, null/*envp*/, dir);
    pipe(process.getErrorStream(), monitor, true);
    pipe(process.getInputStream(), monitor, false);
    return process;
  }

  public static void execAndWait(final String command, final File dir, final IProgressMonitor monitor) throws IOException {
    monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("Executing command: \"%s\" in %s", command, dir)));
    final String[] envp = makeEnv(System.getenv());
    Process process = Runtime.getRuntime().exec(makeArguments(command), envp, dir);
    final StringBuffer errBuffer = new StringBuffer();
    final StringBuffer outBuffer = new StringBuffer();
    final Thread errReader = pipe(process.getErrorStream(), monitor, true);
    final Thread outReader = pipe(process.getInputStream(), monitor, false);
    try {
      int result = process.waitFor();
      // wait for readers to finish...
      errReader.join();
      outReader.join();
      process.getErrorStream().close();
      process.getInputStream().close();
      monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("stdout:\n%s", outBuffer.toString())));
      if (result != 0 || (errBuffer != null && errBuffer.length() > 0)) {
        monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("stderr:\n%s", errBuffer.toString())));
        monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("env: %s", Arrays.toString(envp))));
        throw new IOException(String.format("%s: command: {\"%s\" in: \"%s\"}, retcode='%d'", errBuffer.toString().trim(), command.trim(), dir.getAbsolutePath(), result));
      }

    } catch (InterruptedException e) {
      throw new IOException(e.getMessage());
    }
  }

  private static Thread pipe(final InputStream inStream, final IProgressMonitor monitor, final boolean error) {
    Thread reader = new Thread(new Runnable() {
      public void run() {
        try {
          int in;
          StringBuilder line = new StringBuilder();
          while ((in = inStream.read()) > -1) {// use
            if (monitor != null) {
              char letter = (char) in;
              if ('\n' == letter || '\r' == letter) {
                if (line.length() > 0) {
                  if (error) {
                    monitor.status(new ProgressStatus(ProgressStatus.ERROR, line.toString()));
                  } else {
                    monitor.status(new ProgressStatus(ProgressStatus.INFO, line.toString()));
                  }
                }
                line.setLength(0);
              } else {
                line.append(letter);
              }
            }
          }
        } catch (IOException e) {
          monitor.status(new ProgressStatus(ProgressStatus.ERROR, e.getMessage(), e));
        }
      }
    });
    reader.start();
    return reader;
  }

  private static String[] makeEnv(Map<String, String> envm) {
    final LinkedList<String> env = new LinkedList<String>();
    for (Map.Entry<String, String> entry : envm.entrySet()) {
      env.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
    }
    return env.toArray(new String[env.size()]);
  }

  private static String/*[]*/makeArguments(final String command) {
    return command;
  }

  //TODO: remove directories!
  public static void delete(File file) {
    if (!file.delete()) {
      file.deleteOnExit();
    }
  }

  public static void copy(final InputStream ins, final OutputStream outs) throws IOException {
    try {
      Thread piper = new Thread(new Runnable() {
        public void run() {
          int in;
          try {
            while ((in = ins.read()) > -1) {
              outs.write(in);
            }
          } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
          }
        }
      }, "Pipe stream");
      piper.start();
      piper.join();
    } catch (Throwable t) {
      final IOException ioe = new IOException(t.getMessage());
      ioe.initCause(t);
      throw ioe;
    }
  }

  public static class URIDownloader {

    /**
     * downloads resolved URL
     */
    public final static File load(URI uri, final File destination, final IProgressMonitor monitor, final Class<?>... urlInterceptors) throws IOException {
      if ("file".equals(uri.getScheme())) {
        monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("Tool located on filesystem '%s'. Do not download.", uri)));
        return new File(uri);
      }
      //TODO: move to Provisioner???
      if ("class".equals(uri.getScheme())) {
        final URI original = uri;
        uri = resolve(uri, monitor);
        if (uri != null) {
          monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("Tool location resolved to '%s'", uri)));
        } else {
          monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("Resolver '%s' does not return any URI", original)));
        }

      }
      //create if required
      if (!destination.exists()) {
        destination.mkdirs();
        destination.createNewFile();
        monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("Temporary '%s' file created", destination)));
      }
      //Remote part
      URL url = uri.toURL();
      try {
        url = intercept(url, monitor, urlInterceptors);
        return loadUrl(url, destination, monitor);
      } catch (Exception e) {
        final IOException ioe = new IOException(e.getMessage());
        ioe.initCause(e);
        throw ioe;
      }

    }

    static URL intercept(final URL url, final IProgressMonitor monitor, final Class<?>... urlIntercepters) {
      if (urlIntercepters != null && urlIntercepters.length > 0) {
        monitor.beginTask("Resolving URL");
        try {
          for (final Class<?> intercepter : urlIntercepters) {
            final URL newUrl = new URL(intercepter.getConstructor(URL.class).newInstance(url).toString());
            if (newUrl != null && !newUrl.equals(url)) {
              return newUrl;
            }
          }
        } catch (Throwable t) {
          monitor.status(new ProgressStatus(ProgressStatus.OK, t.getMessage(), t));
        } finally {
          monitor.done();
        }
      }
      return url;
    }

    public static URI resolve(final URI uri, IProgressMonitor monitor) {
      if ("class".equals(uri.getScheme())) {
        String className = uri.getPath() != null && uri.getPath().trim().length() > 0 ? uri.getPath() : uri.getSchemeSpecificPart();
        if (className != null && className.length() > 0) {
          int i = 0;
          while (i < className.length()) {
            if (Character.isLetter(className.charAt(i))) {
              break;
            }
            i++;
          }
          if (i < className.length()) {
            className = className.substring(i, className.length());
            try {
              final Class<?> clazz = Class.forName(className);
              return new URI(clazz.newInstance().toString());
            } catch (Exception e) {
              monitor.status(new ProgressStatus(ProgressStatus.ERROR, e.getMessage(), e));
            }
          }
        }
      }
      monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("Nothing resolve: '%s'", uri)));
      return uri;
    }

    /**
     * download the URL cut from 'swabra'
     */
    private static final int DOWNLOAD_TRY_NUMBER = 10;
    private static final int UI_STEPS_COUNT = 5;

    private final static File loadUrl(final URL source, final File dest, final IProgressMonitor monitor) throws IOException {
      monitor.status(new ProgressStatus(ProgressStatus.OK, new StringBuilder("Downloading object from ").append(source).append(" to ").append(dest.getAbsolutePath()).append("...").toString()));
      HttpURLConnection connection = (HttpURLConnection) source.openConnection();
      final int totalContentLength = connection.getContentLength();
      final int uiStepTick = (int) totalContentLength / UI_STEPS_COUNT;
      monitor.beginTask(String.format("Download"));
      monitor.status(new ProgressStatus(ProgressStatus.INFO, String.format("Loading artifact(s) from '%s'", source)));
      monitor.status(new ProgressStatus(ProgressStatus.INFO, String.format("Size reported: %s bytes", totalContentLength)));
      try {
        long loadedBytes = 0;
        long uiStepLoadedBytes = 0;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        int threshold = DOWNLOAD_TRY_NUMBER;
        boolean cancelled = false;
        while (threshold > 0 && !cancelled) {
          IOException result = null;
          try {
            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(dest);
            final byte[] buffer = new byte[10 * 1024];
            int count;
            while ((count = inputStream.read(buffer)) > 0) {
              outputStream.write(buffer, 0, count);
              loadedBytes += count;
              uiStepLoadedBytes += count;
              if (uiStepLoadedBytes >= uiStepTick) {
                uiStepLoadedBytes = 0;
                monitor.status(new ProgressStatus(ProgressStatus.INFO, String.format("%d (Ok)", loadedBytes, totalContentLength)));
              }
              //check build is interrupted
              if (monitor.isCancelled()) {
                monitor.status(new ProgressStatus(ProgressStatus.OK, "Cancel signal recieved"));
                cancelled = true;
                break;
              }
            }
          } catch (IOException e) {
            result = e;
          } finally {
            try {
              close(inputStream);
              close(outputStream);
            } catch (IOException e) {
              result = e;
            }
          }
          if (result == null && !cancelled) {
            monitor.status(new ProgressStatus(ProgressStatus.OK, new StringBuilder("Successfully downloaded object from ").append(source).append(" to ").append(dest.getAbsolutePath()).toString()));
            monitor.status(new ProgressStatus(ProgressStatus.INFO, String.format("%d downloaded", loadedBytes)));
            return dest;
          }
          --threshold;
          loadedBytes = 0;
        }
        if (cancelled) {
          monitor.status(new ProgressStatus(ProgressStatus.INFO, "Download interrupted."));
          return dest;
        }

      } finally {
        monitor.done();
        connection.disconnect();
      }
      throw new IOException(new StringBuilder("Unable to download object from ").append(source).append(" to ").append(dest.getAbsolutePath()).append(" from ").append(DOWNLOAD_TRY_NUMBER).append(" tries").toString());
    }

    private static void close(Closeable c) throws IOException {
      if (c != null) {
        c.close();
      }
    }

  }

  public static final IProgressMonitor CONSOLE_MONITOR = new IProgressMonitor() {

    Stack<String> myTasks = new Stack<String>();
    private boolean isCanceled;

    public void beginTask(String taskName) {
      myTasks.push(taskName);
      status(new ProgressStatus(IProgressStatus.OK, "started"));
    }

    public void done() {
      status(new ProgressStatus(IProgressStatus.OK, "done"));
      if (!myTasks.isEmpty()) {
        myTasks.pop();
      }
    }

    public void status(IProgressStatus status) {
      if (status.isOK()) {
        System.out.println(String.format("[%s] %s", myTasks.isEmpty() ? " " : myTasks.peek(), status.getMessage()));

      } else {
        System.err.println(String.format("[%s] %s", myTasks.isEmpty() ? " " : myTasks.peek(), status.getMessage()));
        if (status.getException() != null) {
          status.getException().printStackTrace();
        }
      }

    }

    public void cancel() {
      isCanceled = true;
    }

    public boolean isCancelled() {
      return isCanceled;
    }

  };
  public static final IProgressMonitor NULL_MONITOR = new IProgressMonitor() {

    public void beginTask(String taskName) {
      // TODO Auto-generated method stub

    }

    public void done() {
      // TODO Auto-generated method stub

    }

    public void status(IProgressStatus status) {
      // TODO Auto-generated method stub

    }

    public void cancel() {
      // TODO Auto-generated method stub

    }

    public boolean isCancelled() {
      // TODO Auto-generated method stub
      return false;
    }

  };

}

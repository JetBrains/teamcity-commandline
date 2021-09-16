package com.jetbrains.teamcity;

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ServerFacadeWrapper {
  @NotNull private final ExecutorService myExecutor;
  @NotNull private Object myServerFacade;

  public ServerFacadeWrapper(@NotNull final ClassLoader classLoader, @NotNull final URL url) throws Exception {
    myExecutor = Executors.newFixedThreadPool(1, new ThreadFactory() {
      @NotNull
      @Override
      public Thread newThread(@NotNull final Runnable r) {
        final Thread thread = new Thread(r, "ServerFacadeWrapper Background Worker");
        thread.setContextClassLoader(classLoader);
        return thread;
      }
    });

    run(new ThrowableRunnable<Exception>() {
      @Override
      public void run() throws Exception {
        myServerFacade = classLoader.loadClass("com.jetbrains.teamcity.Server").getConstructor(URL.class).newInstance(url);
      }
    });
  }

  public void connect() throws Exception {
    run(new ThrowableRunnable<Exception>() {
      @Override
      public void run() throws Exception {
        myServerFacade.getClass().getMethod("connect").invoke(myServerFacade);
      }
    });
  }

  public void logon(@NotNull final String username, @NotNull final String password) throws Exception {
    run(new ThrowableRunnable<Exception>() {
      @Override
      public void run() throws Exception {
        myServerFacade.getClass()
          .getMethod("logon", String.class, String.class)
          .invoke(myServerFacade, username, password);
      }
    });
  }

  public long getCurrentUser() throws Exception {
    return run(new ThrowableComputable<Integer, Exception>() {
      @Override
      public Integer compute() throws Exception {
        return (Integer)myServerFacade.getClass().getMethod("getCurrentUser").invoke(myServerFacade);
      }
    });
  }

  public void dispose() {
    myExecutor.shutdown();
  }

  /*****************************************************************************************************************/

  private void run(@NotNull final ThrowableRunnable<Exception> action) throws Exception {
    final Ref<Exception> errorRef = new Ref<Exception>();

    myExecutor.submit(new Runnable() {
      @Override
      public void run() {
        try {
          action.run();
        }
        catch (final Exception e) {
          errorRef.set(e);
        }
      }
    }).get();

    final Exception e = errorRef.get();
    if (e != null) {
      throw e;
    }
  }

  private <T> T run(@NotNull final ThrowableComputable<T, Exception> action) throws Exception {
    final Ref<Exception> errorRef = new Ref<Exception>();

    final T result = myExecutor.submit(new Callable<T>() {
      @Override
      public T call() {
        try {
          return action.compute();
        }
        catch (final Exception e) {
          errorRef.set(e);
          return null;
        }
      }
    }).get();

    final Exception e = errorRef.get();
    if (e != null) {
      throw e;
    }

    return result;
  }
}

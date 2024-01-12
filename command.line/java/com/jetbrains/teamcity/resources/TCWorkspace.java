
package com.jetbrains.teamcity.resources;

import com.jetbrains.teamcity.Debug;
import com.jetbrains.teamcity.Storage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;

public class TCWorkspace {

  public static final String TCC_ADMIN_FILE = ".teamcity-mappings.properties";

  static final String TCC_GLOBAL_ADMIN_FILE = new File(System.getProperty("user.home"), Storage.TC_CLI_HOME + File.separator + TCC_ADMIN_FILE).getAbsolutePath();

  private final HashMap<File, ITCResourceMatcher> myCache = new HashMap<File, ITCResourceMatcher>();

  @SuppressWarnings("FieldMayBeFinal") private ITCResourceMatcher myGlobalMatcher;

  private final ITCResourceMatcher myOverridingMatcher;

  public TCWorkspace() {
    this(null);
  }

  public TCWorkspace(final ITCResourceMatcher externMatcher) {
    // setup global admin
    final File defaultConfig = getGlobalAdminFile();
    if (defaultConfig != null && defaultConfig.exists()) {
      myGlobalMatcher = new FileBasedMatcher(defaultConfig);
    } else {
      Debug.getInstance().debug(TCWorkspace.class, MessageFormat.format("Default Admin file \"{0}\" is not found", defaultConfig));
    }
    myOverridingMatcher = externMatcher;
    Debug.getInstance().debug(TCWorkspace.class, String.format("Overriding Matcher set to \"%s\"", externMatcher)); //$NON-NLS-1$
  }

  protected File getGlobalAdminFile() {
    final String globalConfigEnv = System.getenv("TC_DEFAULT_CONFIG");
    if (globalConfigEnv != null && globalConfigEnv.length() > 0) {
      Debug.getInstance().debug(TCWorkspace.class, String.format("Check Environment variable \"%s\" for Admin file ", globalConfigEnv));
      return new File(globalConfigEnv);
    }
    final String myDefaultConfigFile = TCC_GLOBAL_ADMIN_FILE;
    Debug.getInstance().debug(TCWorkspace.class, String.format("Check Default location \"%s\" for Admin file ", myDefaultConfigFile));
    return new File(myDefaultConfigFile);
  }

  static ITCResourceMatcher getMatcherFor(@NotNull final File local) throws IllegalArgumentException {
    // per-folder search
    try {
      File folder = local.getParentFile();
      if (folder != null) {
        folder = folder.getCanonicalFile().getAbsoluteFile();
        final File adminFile = new File(folder, TCC_ADMIN_FILE);
        if (adminFile.exists()) {
          Debug.getInstance().debug(TCWorkspace.class, String.format("found mapping for %s in %s", local, adminFile));
          return new FileBasedMatcher(adminFile);
        } else {
          return getMatcherFor(folder);
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    return null;
  }

  public ITCResource getTCResource(@NotNull File local) throws IllegalArgumentException {
    try {
      local = local.getAbsoluteFile().getCanonicalFile();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    ITCResourceMatcher matcher;
    // set to OverridingMatcher if defined
    if (myOverridingMatcher != null) {
      matcher = myOverridingMatcher;
    } else {
      // look into cache(cache keep file for files resides in same folder only)
      matcher = myCache.get(local.getParentFile());
      if (matcher == null) {
        // seek for uncached
        matcher = getMatcherFor(local);
        if (matcher == null) {
          // look into Global
          matcher = myGlobalMatcher;
          if (myGlobalMatcher == null) {
            Debug.getInstance().debug(TCWorkspace.class, MessageFormat.format("Neither Local nor Global admin files found for \"{0}\"", local));
            return null;
          }
        }
      }
    }
    // cache found
    myCache.put(local.getParentFile(), matcher);
    final ITCResourceMatcher.Matching matching = matcher.getMatching(local);
    if (matching == null) {
      Debug.getInstance().debug(TCWorkspace.class, MessageFormat.format("No Matching found for \"{0}\"", local));
      return null;
    }
    // All found
    final String prefix = matching.getTCID();
    final String relativePath = matching.getRelativePath();
    return new TCResource(local, MessageFormat.format("{0}/{1}", prefix, relativePath)); //$NON-NLS-1$
  }

  static class TCResource implements ITCResource {

    private final File myLocal;
    private final String myRepositoryPath;

    TCResource(File local, String repositoryPath) {
      myLocal = local;
      myRepositoryPath = repositoryPath;
    }

    public File getLocal() {
      return myLocal;
    }

    public String getRepositoryPath() {
      return myRepositoryPath;
    }

    @Override
    public String toString() {
      return MessageFormat.format("local={0}, repo={1}", getLocal(), getRepositoryPath()); //$NON-NLS-1$
    }

  }

}
/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.jetbrains.teamcity.command;

import com.jetbrains.teamcity.*;
import com.jetbrains.teamcity.Util.IFileFilter;
import com.jetbrains.teamcity.resources.FileBasedMatcher;
import com.jetbrains.teamcity.resources.ITCResource;
import com.jetbrains.teamcity.resources.ITCResourceMatcher;
import com.jetbrains.teamcity.resources.TCWorkspace;
import java.io.*;
import java.util.*;
import javax.naming.directory.InvalidAttributesException;
import jetbrains.buildServer.*;
import jetbrains.buildServer.core.runtime.IProgressMonitor;
import jetbrains.buildServer.core.runtime.IProgressStatus;
import jetbrains.buildServer.core.runtime.ProgressStatus;
import jetbrains.buildServer.serverSide.userChanges.PersonalChangeCommitDecision;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.ThreadUtil;
import jetbrains.buildServer.util.filters.Filter;
import jetbrains.buildServer.vcs.patches.LowLevelPatchBuilder;
import jetbrains.buildServer.vcs.patches.LowLevelPatchBuilderImpl;
import jetbrains.buildServer.vcs.patches.PatchBuilderImpl;
import org.jetbrains.annotations.NotNull;

import static java.text.MessageFormat.format;
import static jetbrains.buildServer.util.CollectionsUtil.filterAndConvertCollection;

public class RemoteRun implements ICommand {

  private static final IFileFilter TCC_FILTER = new IFileFilter() {

    public Collection<File> accept(Collection<File> files) {
      final HashSet<File> result = new HashSet<File>();
      for (final File file : files) {
        if (!file.getName().toLowerCase().equals(TCWorkspace.TCC_ADMIN_FILE)) {
          result.add(file);
        }
      }
      return result;
    }
  };

  private static final int SLEEP_INTERVAL = 1000 * 5;
  private static final int DEFAULT_TIMEOUT = 1000 * 60 * 60;

  static final String ID = getMsg("RemoteRun.command.id");

  static final String OVERRIDING_MAPPING_FILE_PARAM = getMsg("RemoteRun.overriding.config.file.argument");

  private static final String CONFIGURATION_PARAM = getMsg("RemoteRun.config.runtime.param");
  static final String CONFIGURATION_PARAM_LONG = getMsg("RemoteRun.config.runtime.param.long");

  static final String PROJECT_PARAM = getMsg("RemoteRun.project.runtime.param");
  static final String PROJECT_PARAM_LONG = getMsg("RemoteRun.project.runtime.param.long");

  static final String MESSAGE_PARAM = getMsg("RemoteRun.message.runtime.param");
  static final String MESSAGE_PARAM_LONG = getMsg("RemoteRun.message.runtime.param.long");

  static final String TIMEOUT_PARAM = getMsg("RemoteRun.timeout.runtime.param");
  private static final String TIMEOUT_PARAM_LONG = getMsg("RemoteRun.timeout.runtime.param.long");

  private static final String NO_WAIT_SWITCH = getMsg("RemoteRun.nowait.runtime.param");
  static final String NO_WAIT_SWITCH_LONG = getMsg("RemoteRun.nowait.runtime.param.long");

  static final String CHECK_FOR_CHANGES_EARLY_SWITCH = getMsg("RemoteRun.checkforchangesearly.runtime.param.long");
  private static final String FORCE_COMPATIBILITY_CHECK_SWITCH = getMsg("RemoteRun.force.compatibility.check.runtime.param.long");
  private static final String FORCE_CLEAN_SWITCH = getMsg("RemoteRun.force.clean.param.long");
  private static final String REBUILD_DEPS_SWITCH = getMsg("RemoteRun.rebuild.dependencies");
  static final String BUILD_PARAM_SWITCH = getMsg("RemoteRun.build.param");

  private Server myServer;
  private String myComment;
  private String myResultDescription;

  private boolean isNoWait = false;

  private long myTimeout;

  private boolean myCleanoff;

  private Map<String, String> myConfigExternal2InternalMap;

  private volatile ECommunicationException myRecentSummaryError;
  private int mySummaryFailureCount;

  static {
    Args.registerArgument(MESSAGE_PARAM, String.format(".*%s\\s+\\S.*", MESSAGE_PARAM)); 
    Args.registerArgument(MESSAGE_PARAM_LONG, String.format(".*%s\\s+\\S.*", MESSAGE_PARAM_LONG)); 

    Args.registerArgument(CONFIGURATION_PARAM, String.format(".*%s\\s+\\S.*", CONFIGURATION_PARAM)); 
    Args.registerArgument(CONFIGURATION_PARAM_LONG, String.format(".*%s\\s+\\S.*", CONFIGURATION_PARAM_LONG)); 

    Args.registerArgument(PROJECT_PARAM, String.format(".*%s\\s?\\S*", PROJECT_PARAM)); 
    Args.registerArgument(PROJECT_PARAM_LONG, String.format(".*%s\\s?\\S*", PROJECT_PARAM_LONG)); 

    Args.registerArgument(TIMEOUT_PARAM, String.format(".*%s\\s+\\d+.*", TIMEOUT_PARAM)); 
    Args.registerArgument(TIMEOUT_PARAM_LONG, String.format(".*%s\\s+\\d+.*", TIMEOUT_PARAM_LONG)); 

    Args.registerArgument(OVERRIDING_MAPPING_FILE_PARAM, String.format(".*%s\\s+\\S.*", OVERRIDING_MAPPING_FILE_PARAM)); 

  }

  public void execute(final Server server, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
    myServer = server;

    // comment
    myComment = args.getArgument(MESSAGE_PARAM, MESSAGE_PARAM_LONG);

    // wait/no wait for build result
    if (args.hasArgument(NO_WAIT_SWITCH, NO_WAIT_SWITCH_LONG)) {
      isNoWait = true;
    }
    // timeout
    if (args.hasArgument(TIMEOUT_PARAM, TIMEOUT_PARAM_LONG)) {
      myTimeout = Long.valueOf(args.getArgument(TIMEOUT_PARAM, TIMEOUT_PARAM_LONG));
    } else {
      myTimeout = DEFAULT_TIMEOUT;
    }
    // do not clean after run
    myCleanoff = args.isCleanOff();

    final TCWorkspace workspace = new TCWorkspace(getOverridingMatcher(args));

    // collect files
    final Collection<File> files = getFiles(args, monitor);

    // collect TC files
    final Collection<ITCResource> tcResources = getTCResources(workspace, files, monitor);

    // prepare patch
    final File patchFile = createPatch(tcResources, monitor);

    // collect configurations for running
    final Collection<String> requestedInternalIds = getRequestedConfigurations(args);
    final Collection<String> internalIds = getApplicableConfigurations(requestedInternalIds, tcResources, monitor, args.hasArgument(FORCE_COMPATIBILITY_CHECK_SWITCH));
    if (internalIds.isEmpty()) {
      throw new IllegalArgumentException(String.format("No one of [%s] configurations affected by collected changes",
                                                       StringUtil.join(",", requestedInternalIds)));
    }
    // prepare changes list
    final long chaneListId = myServer.createChangeList(patchFile, myComment, monitor);

    Map<String, String> parameterMap = convertToMapAndUnescape(args.getArgValues(BUILD_PARAM_SWITCH));

    // fire RR
    scheduleRemoteRun(internalIds, chaneListId,
                      args.hasArgument(CHECK_FOR_CHANGES_EARLY_SWITCH),
                      args.hasArgument(FORCE_CLEAN_SWITCH),
                      args.hasArgument(REBUILD_DEPS_SWITCH),
                      parameterMap,
                      monitor);

    // process result
    if (isNoWait) {
      myResultDescription = String.format("Build for Change %d scheduled successfully", chaneListId);

    } else {
      waitForSuccessResult(chaneListId, myTimeout, monitor);
      myResultDescription = String.format("Build for Change %d run successfully", chaneListId);
    }
  }

  /**
   * @param paramValues list of name=value pairs
   * @return
   */
  static Map<String, String> convertToMapAndUnescape(@NotNull List<String> paramValues) {
    final Map<String, String> result = new HashMap<String, String>();
    for (String s : paramValues) {
      final int idx = s.indexOf("=");
      if (idx > 0) {
        String name = s.substring(0, idx).trim();
        if (StringUtil.isEmpty(name)) continue;

        // Allow escaping of \n with |n
        String value = s.substring(idx + 1)
                        .replaceAll("\\|\\|", "#@TCC@#")
                        .replaceAll("\\|n", "\n")
                        .replaceAll("#@TCC@#", "|");

        result.put(name, value);
      }
    }
    return result;
  }

  /**
   * @return A string of joined build configuration internal IDs which should be used to start the build
   */
  private Collection<String> getRequestedConfigurations(final Args args) throws ECommunicationException {

    final String projectId = args.getArgument(PROJECT_PARAM, PROJECT_PARAM_LONG);
    if (projectId != null) {
      return getBuildTypeInternalIds(projectId);
    }

    final String buildTypeIds = args.getArgument(CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG);
    return convertExternalId2InternalId(buildTypeIds);
  }

  /**
   * @param projectId Could be internal or external ID
   */
  private List<String> getBuildTypeInternalIds(final String projectId) throws ECommunicationException {
    final List<String> result = filterAndConvertCollection(myServer.getConfigurations(), new Converter<String, BuildTypeData>() {
                                                             public String createFrom(@NotNull final BuildTypeData source) {
                                                               return source.getId();
                                                             }
                                                           }, new Filter<BuildTypeData>() {
                                                             public boolean accept(@NotNull final BuildTypeData data) {
                                                               return projectId.equals(data.getProjectId()) || projectId.equals(data.getProjectExternalId());
                                                             }
                                                           }
    );

    if (result.size() == 0 && StringUtil.isNotEmpty(projectId)) {
      throw new IllegalArgumentException(String.format("Cannot find any relevant configurations for project with id [%s]", projectId));
    }

    return result;
  }

  private List<String> convertExternalId2InternalId(final String buildTypeIds) throws ECommunicationException {
    final Collection<String> ids = parseConfigurations(buildTypeIds);
    final ArrayList<String> result = new ArrayList<String>();
    for (String id : ids) {
      final String internalId = getExternal2InternalMap().get(id);
      if (internalId != null) {
        result.add(internalId);
      }
      else if (id.matches("bt\\d+")) {
        result.add(id);
      }
    }

    if (ids.size() > 0 && result.size() == 0) {
      throw new IllegalArgumentException(String.format("Cannot find any relevant configuration ids for [%s]", buildTypeIds));
    }

    return result;
  }

  private Map<String, String> getExternal2InternalMap() throws ECommunicationException {
    if (myConfigExternal2InternalMap == null) {
      myConfigExternal2InternalMap = new HashMap<String, String>();
      for (BuildTypeData configuration : myServer.getConfigurations()) {
        myConfigExternal2InternalMap.put(configuration.getExternalId(), configuration.getId());
      }
    }
    return myConfigExternal2InternalMap;
  }


  Collection<ITCResource> getTCResources(final TCWorkspace workspace, final Collection<File> files, final IProgressMonitor monitor) throws IllegalArgumentException {
    monitor.beginTask(getMsg("RemoteRun.mapping.step.name"));
    final HashSet<ITCResource> out = new HashSet<ITCResource>(files.size());
    for (final File file : files) {
      final ITCResource resource = workspace.getTCResource(file);
      if (resource != null && resource.getRepositoryPath() != null) {
        out.add(resource);
      } else {
        debug("? \"%s\" has not associated ITCResource(%s) or empty RepositoryPath(%s)", file, resource, resource != null ? resource.getRepositoryPath() : null);
      }
    }
    // fire exception if nothing found
    if (out.isEmpty()) {
      throw new IllegalArgumentException(String.format(getMsg("RemoteRun.no.one.mappings.found.error.message"), files.size()));
    }
    monitor.status(new ProgressStatus(IProgressStatus.INFO, String.format(getMsg("RemoteRun.mapping.step.done.message"), out.size(), files.size())));
    monitor.done(); 
    return out;
  }

  File createPatch(Collection<ITCResource> resources, IProgressMonitor monitor) throws ECommunicationException {
    try {
      final File emptyPatchFile = createPatchFile();
      final File patch = fillPatch(emptyPatchFile, resources, monitor);
      if (!myCleanoff) {
        patch.deleteOnExit();
      }
      debug("Patch %s filled with %d bytes", patch.getAbsolutePath(), patch.length());
      return patch;

    } catch (IOException e) {
      throw new ECommunicationException(e);
    }
  }

  ITCResourceMatcher getOverridingMatcher(final Args args) {
    if (args.hasArgument(OVERRIDING_MAPPING_FILE_PARAM)) {
      return new FileBasedMatcher(new File(args.getArgument(OVERRIDING_MAPPING_FILE_PARAM)));

    }
    return null;
  }

  private Collection<String> getApplicableConfigurations(Collection<String> requestedIDs,
                                                         final Collection<ITCResource> files,
                                                         final IProgressMonitor monitor,
                                                         final boolean forceCompatibilityCheck) throws ECommunicationException {

    monitor.beginTask("Collecting configurations for running");

    try {
      if (!requestedIDs.isEmpty()) {
        debug("Requested configurations for running: %s", requestedIDs);
        List<String> intersection = new ArrayList<String>(requestedIDs);

        if (forceCompatibilityCheck) {
          // make intersection of passed and applicable

          final Collection<String> applicable = collectApplicableConfigurations(monitor, files);
          debug("Comparing with applicable configurations: %s", applicable);

          intersection.retainAll(applicable);
          debug("Use configurations for running: %s", intersection);
        }

        return intersection;
      }
      else {
        // if specific configurations are not specified, run on all applicable configurations
        final Collection<String> applicable = collectApplicableConfigurations(monitor, files);
        debug("Using all applicable configurations for running: %s", applicable);
        return applicable;
      }

    }
    finally {
      monitor.done();
    }
  }

  @NotNull
  private Collection<String> collectApplicableConfigurations(final IProgressMonitor monitor, final Collection<ITCResource> files) throws ECommunicationException {

    final HashSet<String> urls = new HashSet<String>();
    for (ITCResource file : files) {
      urls.add(file.getRepositoryPath());
    }

    final Set<String> buffer = new TreeSet<String>(myServer.getApplicableConfigurations(urls));
    monitor.status(new ProgressStatus(IProgressStatus.INFO, format(getMsg("RemoteRun.collected.configuration.done.pattern"), buffer.size(), buffer)));
    return buffer;
  }

  static Collection<String> parseConfigurations(final String cfgId) {
    if (cfgId == null) {
      return Collections.emptyList();
    }
    final String[] configs = cfgId.trim().split(",");
    final HashSet<String> out = new HashSet<String>(configs.length);
    for (final String config : configs) {
      final String trimed = config.trim();
      if (trimed.length() > 0) {
        out.add(trimed);
      }
    }
    return Collections.unmodifiableSet(out);
  }

  private void waitForSuccessResult(final long changeListId, final long timeOut, IProgressMonitor monitor) throws ERemoteError {
    sleep(3000);
    monitor.beginTask("Waiting for Remote Run to finish");
    final long startTime = System.currentTimeMillis();
    UserChangeStatus prevCurrentStatus = null;
    while ((System.currentTimeMillis() - startTime) < timeOut) {

      final List<UserChangeInfoData> personalChanges = getPersonalChanges();
      if (personalChanges == null) {
        if (processFailureAndContinue()) {
          continue;
        }
        else {
          throw new RuntimeException("Error obtaining remote run status after multiple attempts", myRecentSummaryError);
        }
      }
      mySummaryFailureCount = 0;

      for (final UserChangeInfoData data : personalChanges) {

        if (data.getPersonalDesc() != null && data.getPersonalDesc().getId() == changeListId) {
          // check builds status
          final UserChangeStatus currentStatus = data.getChangeStatus();
          if (!currentStatus.equals(prevCurrentStatus)) {
            prevCurrentStatus = currentStatus;

            System.out.print(getBuildStatusDescription(currentStatus));
          }

          if (UserChangeStatus.FAILED_WITH_RESPONSIBLE == currentStatus || UserChangeStatus.FAILED == currentStatus || UserChangeStatus.CANCELED == currentStatus) {
            System.out.println();
            throw new ERemoteError("Remote Run failed: build status=" + getBuildStatusDescription(currentStatus));
          }

          if (UserChangeStatus.RUNNING_FAILED == currentStatus) {
            System.out.println("Remote Run failed: build status=" + getBuildStatusDescription(currentStatus));
          }

          if (UserChangeStatus.CHECKED == currentStatus) {
            // Successful finish
            System.out.println();
            // OK
            monitor.done();
            return;
          }

        }

      }
      System.out.print(".");
      ThreadUtil.sleep(SLEEP_INTERVAL);
    }
    // so, timeout exceed
    throw new RuntimeException(String.format("Stopped waiting for Remote Run %s, timeout exceeded: %dms", myTimeout, changeListId));
  }

  private boolean processFailureAndContinue() {
    mySummaryFailureCount ++;
    if (mySummaryFailureCount <= 7) {
      final int seconds = 2 << mySummaryFailureCount;
      System.out.println(" Failure accessing server [" + myRecentSummaryError.getMessage() + "]; next attempt in " + seconds + " seconds");
      ThreadUtil.sleep(1000 * seconds);
      return true;
    }
    return false;
  }

  private List<UserChangeInfoData> getPersonalChanges() {
    try {
      return myServer.getSummary().getPersonalChanges();
    } catch (ECommunicationException e) {
      Debug.getInstance().error(getClass(), "Unable to get data summary from server", e);
      myRecentSummaryError = e;
      return null;
    }
  }

  @NotNull
  private static String getMsg(final String key) {
    return Messages.getString(key);
  }

  private void sleep(int millis) {
    debug("Falling asleep for [%s] millis...", millis);
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      debug(e.getMessage());
    }

  }

  private void debug(final String format, Object ... data) {
    Debug.getInstance().debug(getClass(), String.format(format, data));
  }

  private Object getCommitStatusDescription(final PersonalChangeCommitDecision commitStatus) {
    return commitStatus;
  }

  private Object getBuildStatusDescription(final UserChangeStatus currentStatus) {

    if (UserChangeStatus.CANCELED == currentStatus) {
      return getMsg("RemoteRun.UserChangeStatus.CANCELED");

    } else if (UserChangeStatus.CHECKED == currentStatus) {
      return getMsg("RemoteRun.UserChangeStatus.CHECKED");

    } else if (UserChangeStatus.FAILED == currentStatus) {
      return getMsg("RemoteRun.UserChangeStatus.FAILED");

    } else if (UserChangeStatus.FAILED_WITH_RESPONSIBLE == currentStatus) {
      return getMsg("RemoteRun.UserChangeStatus.FAILED_WITH_RESPONSIBLE");

    } else if (UserChangeStatus.PENDING == currentStatus) {
      return getMsg("RemoteRun.UserChangeStatus.PENDING");

    } else if (UserChangeStatus.RUNNING_FAILED == currentStatus) {
      return getMsg("RemoteRun.UserChangeStatus.RUNNING_FAILED");

    } else if (UserChangeStatus.RUNNING_SUCCESSFULY == currentStatus) {
      return getMsg("RemoteRun.UserChangeStatus.RUNNING_SUCCESSFULLY");

    }
    return currentStatus;
  }

  private void scheduleRemoteRun(final Collection<String> internalBtIds,
                                 final long changeId,
                                 boolean checkForChangesEarly,
                                 final boolean forceCleanCheckout,
                                 boolean rebuildDependencies,
                                 @NotNull Map<String, String> parameterMap,
                                 @NotNull IProgressMonitor monitor) throws ECommunicationException, ERemoteError {
    final ArrayList<AddToQueueRequest> batch = new ArrayList<AddToQueueRequest>();
    for (final String internalBtId : internalBtIds) {
      final AddToQueueRequest request = new AddToQueueRequest(internalBtId, changeId);
      request.setCheckForChangesEarly(checkForChangesEarly);
      request.setCleanSources(forceCleanCheckout);
      request.setRebuildDependencies(rebuildDependencies);
      request.setBuildParameters(parameterMap);
      batch.add(request);
      final String debugMessage = String.format("Created build request for \"%s\" configuration of changeId=%s, checkForChangesEarly=%s, forceCleanCheckout=%s", internalBtId, changeId, checkForChangesEarly, forceCleanCheckout);
      debug(debugMessage);
    }
    monitor.beginTask("Scheduling personal build");
    final AddToQueueResult result = myServer.addRemoteRunToQueue(batch);

    processSchedulingResult(internalBtIds, result);

    monitor.done();
  }

  private void processSchedulingResult(final Collection<String> internalBtIds, final AddToQueueResult result) throws ERemoteError {
    int addedSuccessfully = 0;

    final StringBuilder errors = new StringBuilder();
    for (final String cfgId : internalBtIds) {
      if (result.isSuccessful(cfgId)) {
        addedSuccessfully++;
      }
      else {
        errors.append(cfgId).append(": ").append(result.getFailureReason(cfgId)).append("\n");
      }
    }

    if (errors.length() > 0) {
      debug("Remote Run scheduling failed: %s", errors.toString());
    }

    if (addedSuccessfully == 0) {
      throw new ERemoteError(errors.toString());
    }

    if (addedSuccessfully > 0) {
      debug("Remote Run has successfully scheduled " + addedSuccessfully + " builds.");
    }
  }

  private File fillPatch(final File patchFile, final Collection<ITCResource> resources, final IProgressMonitor monitor) throws IOException {
    DataOutputStream os = null;
    LowLevelPatchBuilderImpl patcher = null;
    final HashSet<String> modifiedResources = new HashSet<String>();
    final HashSet<String> deletedResources = new HashSet<String>();
    try {
      monitor.beginTask("Preparing patch");
      os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(patchFile)));
      patcher = new LowLevelPatchBuilderImpl(os);
      for (final ITCResource resource : resources) {
        // threat file which is not exist as deleted
        if (resource.getLocal().exists()) {
          debug("+ %s", resource.getRepositoryPath());
          final LowLevelPatchBuilder.WriteFileContent content = new PatchBuilderImpl.StreamWriteFileContent(new BufferedInputStream(new FileInputStream(resource.getLocal())), resource.getLocal().length());
          patcher.changeBinary(resource.getRepositoryPath(), (int) resource.getLocal().length(), content, false);
          modifiedResources.add(resource.getLocal().getPath());

        } else {
          debug("- %s", resource.getRepositoryPath());
          patcher.delete(resource.getRepositoryPath(), true, false);
          deletedResources.add(resource.getLocal().getPath());

        }
      }

    } finally {// finalize patching
      if (patcher != null) {
        patcher.exit(""); 
        patcher.close();
      }
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          //
        }
      }
      final StringBuilder patchingResult = new StringBuilder();
      if (!modifiedResources.isEmpty()) {
        patchingResult.append(String.format("%d new/modified file(s)", modifiedResources.size()));
        if (!deletedResources.isEmpty()) {
          patchingResult.append(", ");
        }
      }
      if (!deletedResources.isEmpty()) {
        patchingResult.append(String.format("%d deleted file(s): %s", deletedResources.size(), deletedResources));
      }
      monitor.status(new ProgressStatus(IProgressStatus.INFO, patchingResult.toString())); 
      monitor.done();
    }
    return patchFile;

  }

  private static File createPatchFile() throws IOException {
    return FileUtil.createTempFile("tcc.jar-", ".patch");
  }

  Collection<File> getFiles(Args args, IProgressMonitor monitor) throws IllegalArgumentException {
    monitor.beginTask(getMsg("RemoteRun.collect.changes.step.name"));
    final String[] elements = args.getArguments();
    int i = 0;// skip command
    while (i < elements.length) {
      final String currentToken = elements[i].toLowerCase();
      if (elements[i].startsWith("-")) { 
        if (elements[i].toLowerCase().equals(CONFIGURATION_PARAM) || elements[i].toLowerCase().equals(CONFIGURATION_PARAM_LONG)) {
          i++; // arg
          if (elements[i].toLowerCase().equals(PROJECT_PARAM) || elements[i].toLowerCase().equals(PROJECT_PARAM_LONG)) {
            i++; // arg
          }
          i++; // arg value
        }
        else if (elements[i].toLowerCase().equals(NO_WAIT_SWITCH) || currentToken.equals(NO_WAIT_SWITCH_LONG) || currentToken.equals(CHECK_FOR_CHANGES_EARLY_SWITCH)) {
          i++; // single token
        }
        else {
          i++; // arg
          i++; // args value
        }
      } else {
        // reach files
        break;
      }
    }

    Collection<File> result;

    if (elements.length > i) {// file's part existing
      final String[] buffer = new String[elements.length - i];
      System.arraycopy(elements, i, buffer, 0, buffer.length);
      debug("Read from arguments: %s", Arrays.toString(buffer));
      result = collectFiles(buffer);
    } else {
      // try read from stdin
      debug("Trying stdin...");
      final String input = readFromStream(System.in);
      if (input != null && input.trim().length() > 0) {
        final String[] buffer = input.split("[\n\r]");
        debug("Read from stdin: %s", Arrays.toString(buffer));
        result = collectFiles(buffer);

      } else { // let's use current directory as root if nothing passed
        debug("Stdin is empty. Will use current (%s) folder as root", new File("."));
        result = TCC_FILTER.accept(Util.SVN_FILES_FILTER.accept(Util.CVS_FILES_FILTER.accept(Util.getFiles("."))));
      }
    }
    if (result.size() == 0) {
      throw new IllegalArgumentException(getMsg("RemoteRun.no.files.collected.for.remoterun.error.message"));
    }
    
    result = normalizePaths(result);

    monitor.status(new ProgressStatus(IProgressStatus.INFO, format(getMsg("RemoteRun.collect.changes.step.result.pattern"), result.size())));
    monitor.done(); 
    for (final File collected : result) {
      debug("%s", collected);
    }
    return result;
  }

  private Collection<File> normalizePaths(final Collection<File> files) {
    TreeSet<File> out = new TreeSet<File>(new Comparator<File>() {
      public int compare(File o1, File o2) {
        return o1.compareTo(o2);
      }
    });
    for (final File file : files) {
      final String trimPath = Util.trim(file.getAbsolutePath(), "\"", "\\", "/");
      out.add(new File(trimPath));
    }
    return out;
  }

  private Collection<File> collectFiles(final String[] elements) {
    final HashSet<File> out = new HashSet<File>();
    for (final String path : elements) {
      final Collection<File> files;
      if (!path.startsWith("@")) { 
        files = Util.getFiles(path);
      } else {
        files = Util.getFiles(new File(path.substring(1, path.length())));
      }
      // filter out system files
      out.addAll(TCC_FILTER.accept(Util.SVN_FILES_FILTER.accept(Util.CVS_FILES_FILTER.accept(files))));
    }
    return out;
  }

  String readFromStream(final InputStream stream) {
    final StringBuilder buffer = new StringBuilder();
    final InputStreamReader in = new InputStreamReader(new BufferedInputStream(stream));
    try {
      if (stream.available() != 0) {
        int n;
        while ((n = in.read()) != -1) {
          buffer.append((char) n);
        }
      }
    } catch (IOException e) {
      Debug.getInstance().error(RemoteRun.class, e.getMessage(), e);
    }
    return buffer.toString();
  }

  public String getId() {
    return ID;
  }

  public boolean isConnectionRequired(final Args args) {
    return true;
  }

  public String getUsageDescription() {
    return String.format(
      getMsg("RemoteRun.help.usage.pattern"),
        getCommandDescription(), getId(), CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG, CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG,
        PROJECT_PARAM, PROJECT_PARAM_LONG, MESSAGE_PARAM, MESSAGE_PARAM_LONG, TIMEOUT_PARAM, TIMEOUT_PARAM_LONG, OVERRIDING_MAPPING_FILE_PARAM,
        NO_WAIT_SWITCH, NO_WAIT_SWITCH_LONG, CHECK_FOR_CHANGES_EARLY_SWITCH, FORCE_COMPATIBILITY_CHECK_SWITCH, FORCE_CLEAN_SWITCH, REBUILD_DEPS_SWITCH, BUILD_PARAM_SWITCH
    );
  }

  public String getCommandDescription() {
    return getMsg("RemoteRun.help.description");
  }

  public String getResultDescription() {
    return myResultDescription;
  }

  public void validate(Args args) throws IllegalArgumentException {
    if (args == null || !args.hasArgument(MESSAGE_PARAM, MESSAGE_PARAM_LONG)) {
      throw new IllegalArgumentException(format(getMsg("RemoteRun.missing.message.para.error.pattern"), MESSAGE_PARAM, MESSAGE_PARAM_LONG));
    }
  }

}

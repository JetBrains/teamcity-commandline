/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import jetbrains.buildServer.*;
import jetbrains.buildServer.core.runtime.IProgressMonitor;
import jetbrains.buildServer.core.runtime.IProgressStatus;
import jetbrains.buildServer.core.runtime.ProgressStatus;
import jetbrains.buildServer.serverSide.userChanges.PersonalChangeCommitDecision;
import jetbrains.buildServer.serverSide.userChanges.PersonalChangeDescriptor;
import jetbrains.buildServer.serverSide.userChanges.PreTestedCommitType;
import jetbrains.buildServer.vcs.patches.LowLevelPatchBuilder;
import jetbrains.buildServer.vcs.patches.LowLevelPatchBuilderImpl;
import jetbrains.buildServer.vcs.patches.PatchBuilderImpl;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import javax.naming.directory.InvalidAttributesException;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.*;

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

  static final int SLEEP_INTERVAL = 1000 * 5;
  static final int DEFAULT_TIMEOUT = 1000 * 60 * 60;

  static final String ID = Messages.getString("RemoteRun.command.id"); //$NON-NLS-1$

  static final String OVERRIDING_MAPPING_FILE_PARAM = Messages.getString("RemoteRun.overriding.config.file.argument"); //$NON-NLS-1$

  static final String CONFIGURATION_PARAM = Messages.getString("RemoteRun.config.runtime.param"); //$NON-NLS-1$
  static final String CONFIGURATION_PARAM_LONG = Messages.getString("RemoteRun.config.runtime.param.long"); //$NON-NLS-1$

  static final String MESSAGE_PARAM = Messages.getString("RemoteRun.message.runtime.param"); //$NON-NLS-1$
  static final String MESSAGE_PARAM_LONG = Messages.getString("RemoteRun.message.runtime.param.long"); //$NON-NLS-1$

  static final String TIMEOUT_PARAM = Messages.getString("RemoteRun.timeout.runtime.param"); //$NON-NLS-1$
  static final String TIMEOUT_PARAM_LONG = Messages.getString("RemoteRun.timeout.runtime.param.long"); //$NON-NLS-1$

  static final String NO_WAIT_SWITCH = Messages.getString("RemoteRun.nowait.runtime.param"); //$NON-NLS-1$
  static final String NO_WAIT_SWITCH_LONG = Messages.getString("RemoteRun.nowait.runtime.param.long"); //$NON-NLS-1$

  static final String PATCHES_FOLDER = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$

  static final String CHECK_FOR_CHANGES_EARLY_SWITCH = Messages.getString("RemoteRun.checkforchangesearly.runtime.param.long"); //$NON-NLS-1$

  private Server myServer;
  private String myComments;
  private String myResultDescription;

  private boolean isNoWait = false;

  private long myTimeout;

  private boolean myCleanoff;

  static {
    Args.registerArgument(MESSAGE_PARAM, String.format(".*%s\\s+[^\\s].*", MESSAGE_PARAM)); //$NON-NLS-1$
    Args.registerArgument(MESSAGE_PARAM_LONG, String.format(".*%s\\s+[^\\s].*", MESSAGE_PARAM_LONG)); //$NON-NLS-1$

    Args.registerArgument(CONFIGURATION_PARAM, String.format(".*%s\\s+[^\\s].*", CONFIGURATION_PARAM)); //$NON-NLS-1$
    Args.registerArgument(CONFIGURATION_PARAM_LONG, String.format(".*%s\\s+[^\\s].*", CONFIGURATION_PARAM_LONG)); //$NON-NLS-1$

    Args.registerArgument(TIMEOUT_PARAM, String.format(".*%s\\s+[0-9]+.*", TIMEOUT_PARAM)); //$NON-NLS-1$
    Args.registerArgument(TIMEOUT_PARAM_LONG, String.format(".*%s\\s+[0-9]+.*", TIMEOUT_PARAM_LONG)); //$NON-NLS-1$

    Args.registerArgument(OVERRIDING_MAPPING_FILE_PARAM, String.format(".*%s\\s+[^\\s].*", OVERRIDING_MAPPING_FILE_PARAM)); //$NON-NLS-1$

  }

  public void execute(final Server server, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
    myServer = server;
    // configuration
    final String cfgId = args.getArgument(CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG);
    // comment
    myComments = args.getArgument(MESSAGE_PARAM, MESSAGE_PARAM_LONG);

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

    final TCWorkspace workspace = new TCWorkspace(Util.getCurrentDirectory(), getOverridingMatcher(args));

    // collect files
    final Collection<File> files = getFiles(args, monitor);

    // collect TC files
    final Collection<ITCResource> tcResources = getTCResources(workspace, files, monitor);

    // prepare patch
    final File patchFile = createPatch(myServer.getURL(), tcResources, monitor);

    // collect configurations for running
    final Collection<String> configurations = getApplicableConfigurations(cfgId, tcResources, monitor);
    if (configurations.isEmpty()) {
      throw new IllegalArgumentException(String.format("No one of [%s] configurations affected by collected changes", cfgId));
    }
    // prepare changes list
    final long chaneListId = createChangeList(myServer.getURL(), myServer.getCurrentUser(), patchFile, monitor);

    // fire RR
    scheduleRemoteRun(configurations, chaneListId, args.hasArgument(CHECK_FOR_CHANGES_EARLY_SWITCH), monitor);

    // process result
    if (isNoWait) {
      myResultDescription = MessageFormat.format(Messages.getString("RemoteRun.schedule.result.ok.pattern"), String.valueOf(chaneListId)); //$NON-NLS-1$

    } else {
      final PersonalChangeDescriptor result = waitForSuccessResult(chaneListId, myTimeout, monitor);
      myResultDescription = MessageFormat.format(Messages.getString("RemoteRun.build.result.ok.pattern"), String.valueOf(chaneListId), result.getPersonalChangeStatus()); //$NON-NLS-1$
    }
  }

  Collection<ITCResource> getTCResources(final TCWorkspace workspace, final Collection<File> files, final IProgressMonitor monitor) throws IllegalArgumentException {
    monitor.beginTask(Messages.getString("RemoteRun.mapping.step.name")); //$NON-NLS-1$
    final HashSet<ITCResource> out = new HashSet<ITCResource>(files.size());
    for (final File file : files) {
      final ITCResource resource = workspace.getTCResource(file);
      if (resource != null && resource.getRepositoryPath() != null) {
        out.add(resource);
      } else {
        Debug.getInstance().debug(RemoteRun.class, String.format("? \"%s\" has not associated ITCResource(%s) or empty RepositoryPath(%s)", //$NON-NLS-1$
            file, resource, resource != null ? resource.getRepositoryPath() : null));
      }
    }
    // fire exception if nothing found
    if (out.isEmpty()) {
      throw new IllegalArgumentException(String.format(Messages.getString("RemoteRun.no.one.mappings.found.error.message"), files.size())); //$NON-NLS-1$
    }
    monitor.status(new ProgressStatus(IProgressStatus.INFO, String.format(Messages.getString("RemoteRun.mapping.step.done.message"), out.size(), files.size())));
    monitor.done(); //$NON-NLS-1$
    return out;
  }

  File createPatch(String url, Collection<ITCResource> resources, IProgressMonitor monitor) throws ECommunicationException {
    try {
      final File emptyPatchFile = createPatchFile(url);
      final File patch = fillPatch(emptyPatchFile, resources, monitor);
      if (!myCleanoff) {
        patch.deleteOnExit();
      }
      Debug.getInstance().debug(this.getClass(), String.format("Patch %s filled with %d bytes", patch.getAbsolutePath(), patch.length())); //$NON-NLS-1$
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

  Collection<String> getApplicableConfigurations(final String cfgId, final Collection<ITCResource> files, final IProgressMonitor monitor) throws ECommunicationException {
    // get all configurations affected by URLs
    final HashSet<String> buffer = new HashSet<String>();
    monitor.beginTask("Collecting configurations for running"); //$NON-NLS-1$
    final HashSet<String> urls = new HashSet<String>();
    for (ITCResource file : files) {
      urls.add(file.getRepositoryPath());
    }
    final Collection<String> applicableConfigurations = myServer.getApplicableConfigurations(urls);
    Debug.getInstance().debug(this.getClass(), String.format("All applicable configurations: %s", applicableConfigurations));
    // make intersection of passed and applicable
    if (cfgId != null) {
      final Collection<String> requestedConfigurations = parseConfigurations(cfgId);
      Debug.getInstance().debug(this.getClass(), String.format("Requested configurations for running: %s", requestedConfigurations));
      if (!requestedConfigurations.isEmpty()) {
        final Collection<String> intersection = Util.intersect(applicableConfigurations, requestedConfigurations);
        Debug.getInstance().debug(this.getClass(), String.format("Use configurations for running: %s", intersection));
        return intersection;
      }
      return requestedConfigurations;
    }
    // if nothing passed run on all applicable
    Debug.getInstance().debug(this.getClass(), String.format("Using all applicable configurations for running", ""));
    buffer.addAll(applicableConfigurations);
    monitor.status(new ProgressStatus(IProgressStatus.INFO, MessageFormat.format(Messages.getString("RemoteRun.collected.configuration.done.pattern"), buffer.size(), buffer))); //$NON-NLS-1$
    monitor.done();
    return buffer;
  }

  static Collection<String> parseConfigurations(final String cfgId) {
    if (cfgId == null) {
      return Collections.<String> emptyList();
    }
    final String[] configs = cfgId.trim().split(",");
    final HashSet<String> out = new HashSet<String>(configs.length);
    for (final String config : configs) {
      final String trimed = config.trim();
      if (trimed.length() > 0) {
        out.add(trimed);
      }
    }
    return Collections.<String> unmodifiableSet(out);
  }

  PersonalChangeDescriptor waitForSuccessResult(final long changeListId, final long timeOut, IProgressMonitor monitor) throws ECommunicationException, ERemoteError {
    sleep(3000);
    monitor.beginTask(Messages.getString("RemoteRun.wait.for.build.step.name")); //$NON-NLS-1$
    final long startTime = System.currentTimeMillis();
    UserChangeStatus prevCurrentStatus = null;
    while ((System.currentTimeMillis() - startTime) < timeOut) {
      final TeamServerSummaryData summary = myServer.getSummary();
      for (final UserChangeInfoData data : summary.getPersonalChanges()) {
        if (data.getPersonalDesc() != null && data.getPersonalDesc().getId() == changeListId) {
          // check builds status
          final UserChangeStatus currentStatus = data.getChangeStatus();
          if (!currentStatus.equals(prevCurrentStatus)) {
            prevCurrentStatus = currentStatus;
            System.out.print(MessageFormat.format(Messages.getString("RemoteRun.wait.for.build.statuschanged.step.name"), getBuildStatusDescription(currentStatus)));
//            monitor.status(new ProgressStatus(IProgressStatus.INFO, MessageFormat.format(Messages.getString("RemoteRun.wait.for.build.statuschanged.step.name"), getBuildStatusDescription(currentStatus)))); //$NON-NLS-1$
//            monitor.worked(); //$NON-NLS-1$
          }
          if (UserChangeStatus.FAILED_WITH_RESPONSIBLE == currentStatus || UserChangeStatus.FAILED == currentStatus || UserChangeStatus.CANCELED == currentStatus) {
            System.out.println();
            throw new ERemoteError(MessageFormat.format(Messages.getString("RemoteRun.build.failed.error.pattern"), getBuildStatusDescription(currentStatus))); //$NON-NLS-1$
          }
          // check commit status
          final PersonalChangeDescriptor descriptor = data.getPersonalDesc();
          PersonalChangeCommitDecision commitStatus = descriptor.getPersonalChangeStatus();
          if (PersonalChangeCommitDecision.COMMIT == commitStatus) {
            System.out.println();            
            // OK
            monitor.done();
            return descriptor;

          } else if (PersonalChangeCommitDecision.DO_NOT_COMMIT == commitStatus) {
            System.out.println();            
            // build is OK, but commit is not allowed
            throw new ERemoteError(MessageFormat.format(Messages.getString("RemoteRun.build.ok.commit.rejected.error.pattern"), getCommitStatusDescription(commitStatus))); //$NON-NLS-1$
          }
        }
      }
      try {
        System.out.print(".");
        Thread.sleep(SLEEP_INTERVAL);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    // so, timeout exceed
    throw new RuntimeException(MessageFormat.format(Messages.getString("RemoteRun.wait.for.build.timeout.exceed.error"), myTimeout, changeListId)); //$NON-NLS-1$
  }

  private void sleep(int millis) {
    Debug.getInstance().debug(this.getClass(), String.format("Falling asleep for [%s] millis...", millis));
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Debug.getInstance().debug(this.getClass(), e.getMessage());
    }

  }

  private Object getCommitStatusDescription(final PersonalChangeCommitDecision commitStatus) {
    return commitStatus;
  }

  private Object getBuildStatusDescription(final UserChangeStatus currentStatus) {

    if (UserChangeStatus.CANCELED == currentStatus) {
      return Messages.getString("RemoteRun.UserChangeStatus.CANCELED"); //$NON-NLS-1$

    } else if (UserChangeStatus.CHECKED == currentStatus) {
      return Messages.getString("RemoteRun.UserChangeStatus.CHECKED"); //$NON-NLS-1$

    } else if (UserChangeStatus.FAILED == currentStatus) {
      return Messages.getString("RemoteRun.UserChangeStatus.FAILED"); //$NON-NLS-1$

    } else if (UserChangeStatus.FAILED_WITH_RESPONSIBLE == currentStatus) {
      return Messages.getString("RemoteRun.UserChangeStatus.FAILED_WITH_RESPONSIBLE"); //$NON-NLS-1$

    } else if (UserChangeStatus.PENDING == currentStatus) {
      return Messages.getString("RemoteRun.UserChangeStatus.PENDING"); //$NON-NLS-1$

    } else if (UserChangeStatus.RUNNING_FAILED == currentStatus) {
      return Messages.getString("RemoteRun.UserChangeStatus.RUNNING_FAILED"); //$NON-NLS-1$

    } else if (UserChangeStatus.RUNNING_SUCCESSFULY == currentStatus) {
      return Messages.getString("RemoteRun.UserChangeStatus.RUNNING_SUCCESSFULLY"); //$NON-NLS-1$

    }
    return currentStatus;
  }

  long scheduleRemoteRun(final Collection<String> configurations, final long changeId, boolean checkForChangesEarly, final IProgressMonitor monitor) throws ECommunicationException, ERemoteError {
    final ArrayList<AddToQueueRequest> batch = new ArrayList<AddToQueueRequest>();
    for (final String cfgId : configurations) {
      final AddToQueueRequest request = new AddToQueueRequest(cfgId, changeId);
      request.setCheckForChangesEarly(checkForChangesEarly);
      batch.add(request);
      final String debugMessage = String.format("Created build request for \"%s\" configuration of changeId=%s, checkForChangesEarly=%s", cfgId, changeId, checkForChangesEarly);
      Debug.getInstance().debug(RemoteRun.class, debugMessage); //$NON-NLS-1$			
    }
    monitor.beginTask(Messages.getString("RemoteRun.scheduling.build.step.name")); //$NON-NLS-1$
    final AddToQueueResult result = myServer.addRemoteRunToQueue(batch);// TODO:
                                                                                    // process
                                                                                    // Result
                                                                                    // here

    if (result.hasFailures()) {
      final StringBuilder errors = new StringBuilder();
      for (final String cfgId : configurations) {
        errors.append(result.getFailureReason(cfgId)).append("\n"); //$NON-NLS-1$
      }
      Debug.getInstance().debug(RemoteRun.class, String.format("Remote Run scheduling failed: %s", errors.toString())); //$NON-NLS-1$			
      throw new ERemoteError(errors.toString());
    } else {
      Debug.getInstance().debug(RemoteRun.class, String.format("Remote Run scheduled successfully.")); //$NON-NLS-1$
    }

    monitor.done();
    return changeId;
  }

  long createChangeList(String serverURL, int userId, final File patchFile, final IProgressMonitor monitor) throws ECommunicationException {
    try {
      monitor.beginTask(Messages.getString("RemoteRun.send.patch.step.name")); //$NON-NLS-1$
      final SimpleHttpConnectionManager manager = new SimpleHttpConnectionManager();
      HostConfiguration hostConfiguration = new HostConfiguration();
      hostConfiguration.setHost(new URI(serverURL, false));
      HttpConnection connection = manager.getConnection(hostConfiguration);

      if (!connection.isOpen()) {
        connection.open();
      }
      String _uri = serverURL;
      if (!serverURL.endsWith("/")) { //$NON-NLS-1$
        _uri += "/"; //$NON-NLS-1$
      }
      _uri += "uploadChanges.html"; //$NON-NLS-1$
      final PostMethod postMethod = new PostMethod(_uri);
      final BufferedInputStream content = new BufferedInputStream(new FileInputStream(patchFile));
      try {
        postMethod.setRequestEntity(new InputStreamRequestEntity(content, patchFile.length()));
        postMethod.setQueryString(new NameValuePair[] { new NameValuePair("userId", String.valueOf(userId)), //$NON-NLS-1$
            new NameValuePair("description", myComments), //$NON-NLS-1$
            new NameValuePair("date", String.valueOf(System.currentTimeMillis())), //$NON-NLS-1$
            new NameValuePair("commitType", String.valueOf(PreTestedCommitType.COMMIT_IF_SUCCESSFUL.getId())), });//TODO: make argument //$NON-NLS-1$
        postMethod.execute(new HttpState(), connection);
      } finally {
        content.close();
      }
      // post requests to queue
      final String response = postMethod.getResponseBodyAsString();
      monitor.status(new ProgressStatus(IProgressStatus.INFO, String.format("sent %d bytes", patchFile.length()))); //$NON-NLS-1$      
      monitor.done();
      return Long.parseLong(response);

    } catch (IOException e) {
      throw new ECommunicationException(e);
    }
  }

  File fillPatch(final File patchFile, final Collection<ITCResource> resources, final IProgressMonitor monitor) throws IOException, ECommunicationException {
    DataOutputStream os = null;
    LowLevelPatchBuilderImpl patcher = null;
    final HashSet<String> modifiedResources = new HashSet<String>();
    final HashSet<String> deletedResources = new HashSet<String>();
    try {
      monitor.beginTask(Messages.getString("RemoteRun.preparing.patch.step.name")); //$NON-NLS-1$
      os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(patchFile)));
      patcher = new LowLevelPatchBuilderImpl(os);
      for (final ITCResource resource : resources) {
        // threat file which is not exist as deleted
        if (resource.getLocal().exists()) {
          Debug.getInstance().debug(RemoteRun.class, String.format("+ %s", resource.getRepositoryPath())); //$NON-NLS-1$
          final LowLevelPatchBuilder.WriteFileContent content = new PatchBuilderImpl.StreamWriteFileContent(new BufferedInputStream(new FileInputStream(resource.getLocal())), resource.getLocal().length());
          patcher.changeBinary(resource.getRepositoryPath(), (int) resource.getLocal().length(), content, false);
          modifiedResources.add(resource.getLocal().getPath());

        } else {
          Debug.getInstance().debug(RemoteRun.class, String.format("- %s", resource.getRepositoryPath())); //$NON-NLS-1$
          patcher.delete(resource.getRepositoryPath(), true, false);
          deletedResources.add(resource.getLocal().getPath());

        }
      }

    } finally {// finalize patching
      if (patcher != null) {
        patcher.exit(""); //$NON-NLS-1$
        patcher.close();
      }
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
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
      monitor.status(new ProgressStatus(IProgressStatus.INFO, patchingResult.toString())); //$NON-NLS-1$
      monitor.done();
    }
    return patchFile;

  }

  static File createPatchFile(String url) {
    File stateLocation = new File(PATCHES_FOLDER == null ? "." : PATCHES_FOLDER); //$NON-NLS-1$
    try {
      url = new String(Base64.encodeBase64(MessageDigest.getInstance("MD5").digest(url.getBytes()))); //$NON-NLS-1$
    } catch (NoSuchAlgorithmException e) {
      //
    }
    File file = new File(stateLocation, url + ".patch"); //$NON-NLS-1$
    file.getParentFile().mkdirs();
    return file;
  }

  Collection<File> getFiles(Args args, IProgressMonitor monitor) throws IllegalArgumentException {
    monitor.beginTask(Messages.getString("RemoteRun.collect.changes.step.name")); //$NON-NLS-1$
    final String[] elements = args.getArguments();
    int i = 0;// skip command
    while (i < elements.length) {
      final String currentToken = elements[i].toLowerCase();
      if (elements[i].startsWith("-")) { //$NON-NLS-1$
        if (elements[i].toLowerCase().equals(NO_WAIT_SWITCH) || currentToken.equals(NO_WAIT_SWITCH_LONG) || currentToken.equals(CHECK_FOR_CHANGES_EARLY_SWITCH)) {
          i++; // single token
        } else {
          i++; // arg
          i++; // args value
        }
      } else {
        // reach files
        break;
      }
    }

    final TreeSet<File> result = new TreeSet<File>(new Comparator<File>() {
      public int compare(File o1, File o2) {
        return o1.compareTo(o2);
      }
    });

    if (elements.length > i) {// file's part existing
      final String[] buffer = new String[elements.length - i];
      System.arraycopy(elements, i, buffer, 0, buffer.length);
      Debug.getInstance().debug(RemoteRun.class, String.format("Read from arguments: %s", Arrays.toString(buffer))); //$NON-NLS-1$
      final Collection<File> files = collectFiles(buffer);
      result.addAll(traverse(files));

    } else {
      // try read from stdin
      Debug.getInstance().debug(RemoteRun.class, "Trying stdin..."); //$NON-NLS-1$
      final String input = readFromStream(System.in);
      if (input != null && input.trim().length() > 0) {
        final String[] buffer = input.split("[\n\r]"); //$NON-NLS-1$
        Debug.getInstance().debug(RemoteRun.class, String.format("Read from stdin: %s", Arrays.toString(buffer))); //$NON-NLS-1$
        final Collection<File> files = collectFiles(buffer);
        result.addAll(traverse(files));

      } else { // let's use current directory as root if nothing passed
        Debug.getInstance().debug(RemoteRun.class, String.format("Stdin is empty. Will use current (%s) folder as root", new File("."))); //$NON-NLS-1$ //$NON-NLS-2$
        result.addAll(traverse(TCC_FILTER.accept(Util.SVN_FILES_FILTER.accept(Util.CVS_FILES_FILTER.accept(Util.getFiles(".")))))); //$NON-NLS-1$

      }
    }
    if (result.size() == 0) {
      throw new IllegalArgumentException(Messages.getString("RemoteRun.no.files.collected.for.remoterun.error.message")); //$NON-NLS-1$
    }
    monitor.status(new ProgressStatus(IProgressStatus.INFO, MessageFormat.format(Messages.getString("RemoteRun.collect.changes.step.result.pattern"), result.size()))); //$NON-NLS-1$
    monitor.done(); //$NON-NLS-1$
    for (final File collected : result) {
      Debug.getInstance().debug(RemoteRun.class, String.format("%s", collected)); //$NON-NLS-1$ //$NON-NLS-2$
    }
    return result;
  }

  Collection<File> traverse(final Collection<File> files) {
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
      if (!path.startsWith("@")) { //$NON-NLS-1$
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
        Messages.getString("RemoteRun.help.usage.pattern"), //$NON-NLS-1$
        getCommandDescription(), getId(), CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG, MESSAGE_PARAM, MESSAGE_PARAM_LONG, TIMEOUT_PARAM, TIMEOUT_PARAM_LONG, NO_WAIT_SWITCH, NO_WAIT_SWITCH_LONG, CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG, MESSAGE_PARAM, MESSAGE_PARAM_LONG,
        TIMEOUT_PARAM, TIMEOUT_PARAM_LONG, NO_WAIT_SWITCH, NO_WAIT_SWITCH_LONG, OVERRIDING_MAPPING_FILE_PARAM);
  }

  public String getCommandDescription() {
    return Messages.getString("RemoteRun.help.description"); //$NON-NLS-1$
  }

  public String getResultDescription() {
    return myResultDescription;
  }

  public void validate(Args args) throws IllegalArgumentException {
    if (args == null || !args.hasArgument(MESSAGE_PARAM, MESSAGE_PARAM_LONG)) {
      throw new IllegalArgumentException(MessageFormat.format(Messages.getString("RemoteRun.missing.message.para.error.pattern"), MESSAGE_PARAM, MESSAGE_PARAM_LONG)); //$NON-NLS-1$
    }
  }

}

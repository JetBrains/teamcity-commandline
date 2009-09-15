package com.jetbrains.teamcity.command;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.naming.directory.InvalidAttributesException;

import jetbrains.buildServer.AddToQueueRequest;
import jetbrains.buildServer.AddToQueueResult;
import jetbrains.buildServer.TeamServerSummaryData;
import jetbrains.buildServer.UserChangeInfoData;
import jetbrains.buildServer.UserChangeStatus;
import jetbrains.buildServer.serverSide.userChanges.PersonalChangeCommitDecision;
import jetbrains.buildServer.serverSide.userChanges.PersonalChangeDescriptor;
import jetbrains.buildServer.serverSide.userChanges.PreTestedCommitType;
import jetbrains.buildServer.vcs.patches.LowLevelPatchBuilder;
import jetbrains.buildServer.vcs.patches.LowLevelPatchBuilderImpl;
import jetbrains.buildServer.vcs.patches.PatchBuilderImpl;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.Util.IFileFilter;
import com.jetbrains.teamcity.resources.FileBasedMatcher;
import com.jetbrains.teamcity.resources.ITCResource;
import com.jetbrains.teamcity.resources.ITCResourceMatcher;
import com.jetbrains.teamcity.resources.TCWorkspace;
import com.jetbrains.teamcity.runtime.IProgressMonitor;


public class RemoteRun implements ICommand {
	
	private static Logger LOGGER = Logger.getLogger(RemoteRun.class) ;	
	
	private static final IFileFilter TCC_FILTER = new  IFileFilter() {
		
		public Collection<File> accept(Collection<File> files) {
			final HashSet<File> result = new HashSet<File>();
			for(final File file : files){
				if(!file.getName().toLowerCase().equals(TCWorkspace.TCC_ADMIN_FILE)){
					result.add(file);
				}
			}
			return result;
		}
	};


	static final int SLEEP_INTERVAL = 1000 * 10;
	static final int DEFAULT_TIMEOUT = 1000 * 60 * 60;

	static final String ID = Messages.getString("RemoteRun.command.id"); //$NON-NLS-1$
	
	static final String CONFIG_FILE_PARAM = "--config-file";
	
	static final String CONFIGURATION_PARAM = Messages.getString("RemoteRun.config.runtime.param"); //$NON-NLS-1$
	static final String CONFIGURATION_PARAM_LONG = Messages.getString("RemoteRun.config.runtime.param.long"); //$NON-NLS-1$
	
	static final String MESSAGE_PARAM = Messages.getString("RemoteRun.message.runtime.param"); //$NON-NLS-1$
	static final String MESSAGE_PARAM_LONG = Messages.getString("RemoteRun.message.runtime.param.long"); //$NON-NLS-1$
	
	static final String TIMEOUT_PARAM = Messages.getString("RemoteRun.timeout.runtime.param"); //$NON-NLS-1$
	static final String TIMEOUT_PARAM_LONG = Messages.getString("RemoteRun.timeout.runtime.param.long"); //$NON-NLS-1$
	
	static final String NO_WAIT_SWITCH = Messages.getString("RemoteRun.nowait.runtime.param"); //$NON-NLS-1$
	static final String NO_WAIT_SWITCH_LONG = Messages.getString("RemoteRun.nowait.runtime.param.long"); //$NON-NLS-1$
	
	static final String NO_USE_SHARES_SWITCH_LONG = Messages.getString("RemoteRun.noshares.runtime.param.long"); //$NON-NLS-1$
	
	static final String PATCHES_FOLDER = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
	
//	private String myConfigurationId;
	private Server myServer;
	private String myComments;
	private boolean isNoWait = false;
	private long myTimeout;
	private String myResultDescription;

	public void execute(final Server server, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		myServer = server;
		//configuration
		final String cfgId = args.getArgument(CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG);
		//comment
		myComments = args.getArgument(MESSAGE_PARAM, MESSAGE_PARAM_LONG);
		//wait/no wait for build result
		if(args.hasArgument(NO_WAIT_SWITCH, NO_WAIT_SWITCH_LONG)){
			isNoWait  = true;
		}
		//timeout
		if(args.hasArgument(TIMEOUT_PARAM, TIMEOUT_PARAM_LONG)){
			myTimeout = Long.valueOf(args.getArgument(TIMEOUT_PARAM, TIMEOUT_PARAM_LONG));
		} else {
			myTimeout = DEFAULT_TIMEOUT;
		}
		final TCWorkspace workspace = new TCWorkspace(Util.getCurrentDirectory(), getOverridingMatcher(args));
		//collect files
		final Collection<File> files = getFiles(args, monitor);
		//associate files to shares(vcsroots)
//		final Map<IShare, ArrayList<File>> rootsMap = getRootMap(server, cfgId, files, args.hasArgument(NO_USE_SHARES_SWITCH_LONG), monitor);
		
		//collect configurations for running
		final Collection<String> configurations = getApplicableConfigurations(cfgId, workspace, files, monitor);
		
		//prepare changes list
		final long chaneListId = createChangeList(myServer.getURL(), myServer.getCurrentUser(), workspace, files, monitor);
		
		//fire RR
		scheduleRemoteRun(configurations, chaneListId, monitor);
		
		//process result
		if(isNoWait){
			myResultDescription = MessageFormat.format(Messages.getString("RemoteRun.schedule.result.ok.pattern"), String.valueOf(chaneListId)); //$NON-NLS-1$

		} else { 
			final PersonalChangeDescriptor result = waitForSuccessResult(chaneListId, myTimeout, monitor);
			myResultDescription = MessageFormat.format(Messages.getString("RemoteRun.build.result.ok.pattern"), String.valueOf(chaneListId), result.getPersonalChangeStatus()); //$NON-NLS-1$
		}
		return;
	}
	
	ITCResourceMatcher getOverridingMatcher(final Args args) {
		if(args.hasArgument(CONFIG_FILE_PARAM)){
			return new FileBasedMatcher(new File(args.getArgument(CONFIG_FILE_PARAM)));
			
		}
		return null;
	}

	Collection<String> getApplicableConfigurations(final String cfgId, final TCWorkspace workspace, final Collection<File> files, final IProgressMonitor monitor) throws ECommunicationException {
		if(cfgId != null){
			return Collections.singletonList(cfgId);
		}
		final HashSet<String> buffer = new HashSet<String>();
		monitor.beginTask("Collecting configurations for running"); //$NON-NLS-1$
		final HashSet<String> resources = new HashSet<String>();
		for(File file : files){
			resources.add(workspace.getTCResource(file).getRepositoryPath());
//			final URLFactory factory = URLFactory.getFactory(entry.getKey());
//			if(factory != null){
//				for(final File file : entry.getValue()){
//					try {
//						final String url = factory.getUrl(file);
//						if(url != null){
//							resources.add(url);			
//						}
//					} catch (IOException e) {
//						throw new ECommunicationException(e);
//					}
//				}
//			}
//			
		}
		buffer.addAll(myServer.getApplicableConfigurations(resources));
		monitor.done(MessageFormat.format(Messages.getString("RemoteRun.collected.configuration.done.pattern"), buffer.size(), buffer)); //$NON-NLS-1$
		return buffer;
	}

	PersonalChangeDescriptor waitForSuccessResult(final long changeListId, final long timeOut, IProgressMonitor monitor) throws ECommunicationException, ERemoteError {
		monitor.beginTask(Messages.getString("RemoteRun.wait.for.build.step.name")); //$NON-NLS-1$
		final long startTime = System.currentTimeMillis();
		UserChangeStatus prevCurrentStatus = null;
		while ((System.currentTimeMillis() - startTime) < timeOut) {
			final TeamServerSummaryData summary = myServer.getSummary();
			for(final UserChangeInfoData data : summary.getPersonalChanges()){
				if(data.getPersonalDesc() != null && data.getPersonalDesc().getId() == changeListId){
					//check builds status 
					final UserChangeStatus currentStatus = data.getChangeStatus();
					if(!currentStatus.equals(prevCurrentStatus)){
						prevCurrentStatus = currentStatus;
						monitor.worked(MessageFormat.format(Messages.getString("RemoteRun.wait.for.build.statuschanged.step.name"), getBuildStatusDescription(currentStatus))); //$NON-NLS-1$
					}
					if (UserChangeStatus.FAILED_WITH_RESPONSIBLE == currentStatus 
							|| UserChangeStatus.FAILED == currentStatus  
							|| UserChangeStatus.CANCELED == currentStatus) {
						throw new ERemoteError(MessageFormat.format(Messages.getString("RemoteRun.build.failed.error.pattern"), getBuildStatusDescription(currentStatus))); //$NON-NLS-1$
					}
					//check commit status
					final PersonalChangeDescriptor descriptor = data.getPersonalDesc();
					PersonalChangeCommitDecision commitStatus = descriptor.getPersonalChangeStatus();
					if(PersonalChangeCommitDecision.COMMIT == commitStatus){
						//OK
						monitor.done();
						return descriptor;

					} else if (PersonalChangeCommitDecision.DO_NOT_COMMIT == commitStatus){
						//build is OK, but commit is not allowed
						throw new ERemoteError(MessageFormat.format(Messages.getString("RemoteRun.build.ok.commit.rejected.error.pattern"), getCommitStatusDescription(commitStatus))); //$NON-NLS-1$
					}
				}
			}
			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		//so, timeout exceed
		throw new RuntimeException(MessageFormat.format(Messages.getString("RemoteRun.wait.for.build.timeout.exceed.error"), myTimeout, changeListId)); //$NON-NLS-1$
	}


	private Object getCommitStatusDescription(final PersonalChangeCommitDecision commitStatus) {
		return commitStatus;
	}

	private Object getBuildStatusDescription(final UserChangeStatus currentStatus) {
		
		if(UserChangeStatus.CANCELED == currentStatus){
			return Messages.getString("RemoteRun.UserChangeStatus.CANCELED"); //$NON-NLS-1$
			
		} else if(UserChangeStatus.CHECKED == currentStatus){
			return Messages.getString("RemoteRun.UserChangeStatus.CHECKED");  //$NON-NLS-1$
			
		} else if(UserChangeStatus.FAILED == currentStatus){
			return Messages.getString("RemoteRun.UserChangeStatus.FAILED"); //$NON-NLS-1$
			
		} else if(UserChangeStatus.FAILED_WITH_RESPONSIBLE == currentStatus){
			return Messages.getString("RemoteRun.UserChangeStatus.FAILED_WITH_RESPONSIBLE"); //$NON-NLS-1$
			
		} else if(UserChangeStatus.PENDING == currentStatus){
			return Messages.getString("RemoteRun.UserChangeStatus.PENDING"); //$NON-NLS-1$
			
		} else if(UserChangeStatus.RUNNING_FAILED == currentStatus){
			return Messages.getString("RemoteRun.UserChangeStatus.RUNNING_FAILED"); //$NON-NLS-1$
			
		} else if(UserChangeStatus.RUNNING_SUCCESSFULY == currentStatus){
			return Messages.getString("RemoteRun.UserChangeStatus.RUNNING_SUCCESSFULY"); //$NON-NLS-1$
			
		}
		return currentStatus;
	}

	long scheduleRemoteRun(final Collection<String> configurations, final long changeId, final IProgressMonitor monitor) throws ECommunicationException, ERemoteError {
//			final TCWorkspace workspace, final Collection<File> files, final IProgressMonitor monitor) throws ECommunicationException, ERemoteError {
//		final int currentUser = myServer.getCurrentUser();
//		try {
//			//prepare change list & patch for remote run
//			final long changeId = createChangeList(myServer.getURL(), currentUser, workspace, files, monitor);
			//schedule for execution and process status
			final ArrayList<AddToQueueRequest> batch = new ArrayList<AddToQueueRequest>();
			for(final String cfgId : configurations){
				final AddToQueueRequest request = new AddToQueueRequest(cfgId, changeId);
				batch.add(request);
			}
			monitor.beginTask(Messages.getString("RemoteRun.scheduling.build.step.name")); //$NON-NLS-1$
			final AddToQueueResult result = myServer.addRemoteRunToQueue(batch, myComments);//TODO: process Result here
			if(result.hasFailures()){
				final StringBuffer errors = new StringBuffer();
				for(final String cfgId : configurations){
					errors.append(result.getFailureReason(cfgId)).append("\n"); //$NON-NLS-1$
				}
				throw new ERemoteError(errors.toString());
			}
			monitor.done();
			return changeId;
//		} catch (IOException e) {
//			throw new ECommunicationException(e);
//		}
		
	}
	
	
	long createChangeList(String serverURL, int userId, final TCWorkspace workspace, final Collection<File> files, final IProgressMonitor monitor) throws ECommunicationException {
		try{
			monitor.beginTask(Messages.getString("RemoteRun.preparing.patch..step.name")); //$NON-NLS-1$
			final File patch = createPatch(createPatchFile(serverURL), workspace, files);
			patch.deleteOnExit();
			monitor.done();

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
			final BufferedInputStream content = new BufferedInputStream(new FileInputStream(patch));
			try {
				postMethod.setRequestEntity(new InputStreamRequestEntity(content, patch.length()));
				postMethod.setQueryString(new NameValuePair[] {
						new NameValuePair("userId", String.valueOf(userId)), //$NON-NLS-1$
						new NameValuePair("description", myComments), //$NON-NLS-1$
						new NameValuePair("date", String.valueOf(System.currentTimeMillis())), //$NON-NLS-1$
						new NameValuePair("commitType", String.valueOf(PreTestedCommitType.COMMIT_IF_SUCCESSFUL.getId())), });//TODO: make argument //$NON-NLS-1$
				postMethod.execute(new HttpState(), connection);
			} finally {
				content.close();
			}
			//post requests to queue
			final String response = postMethod.getResponseBodyAsString();
			monitor.done();
			return Long.parseLong(response);
			
		} catch (IOException e){
			throw new ECommunicationException(e);
		}
	}

	File createPatch(final File patchFile, final TCWorkspace workspace, final Collection<File> files) throws IOException, ECommunicationException {
		DataOutputStream os = null;
		LowLevelPatchBuilderImpl patcher = null;
		try{
			os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(patchFile)));
			patcher = new LowLevelPatchBuilderImpl(os);
			for(final File file : files){
				LowLevelPatchBuilder.WriteFileContent content = new PatchBuilderImpl.StreamWriteFileContent(new BufferedInputStream(new FileInputStream(file)), file.length());
				final ITCResource resource = workspace.getTCResource(file);
				if(resource != null && resource.getRepositoryPath() != null){
					LOGGER.debug(MessageFormat.format("+ {0}", resource.getRepositoryPath())); //$NON-NLS-1$
					if(file.exists()){
						patcher.changeBinary(resource.getRepositoryPath(), (int) file.length(), content, false);
					} else {
						patcher.delete(resource.getRepositoryPath(), true, false);
					}
				} else {
					LOGGER.debug(MessageFormat.format("? {0}", resource.getRepositoryPath())); //$NON-NLS-1$
				}
			}
			//
		} finally{
			patcher.exit(""); //$NON-NLS-1$
			if (patcher != null) {
				patcher.close();
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
				}
			}
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

//	@Deprecated
//	private Map<IShare, ArrayList<File>> getRootMap(final Server server, final String cfgId, 
//			final Collection<File> files, final boolean doNotUseShares, IProgressMonitor monitor) throws IllegalArgumentException, ECommunicationException  {
//		
//		//if switch is set do not use any sharing info
//		if(doNotUseShares && cfgId != null){//can not provide service without any info vcsroots about(can be obtained from cfg)
//			LOGGER.debug(MessageFormat.format("\"{0}\" detected. Will not use any persisted Sahre's Info for VcsRoot binding", NO_USE_SHARES_SWITCH_LONG)); //$NON-NLS-1$
//			return Collections.singletonMap(createInplaceShare(server, cfgId), new ArrayList<File>(files));
//		}
//		//use stored sharing info for associate resources to vcsroots
//		try{
//			final HashMap<IShare, ArrayList<File>> result = new HashMap<IShare, ArrayList<File>>();
//			final TCAccess access = TCAccess.getInstance();
//			if(access.roots().isEmpty()){
//				throw new IllegalArgumentException(Messages.getString("RemoteRun.no.shares.for.remoterun.error.message")); //$NON-NLS-1$
//			}
//			for(final File file : files){
//				final IShare root = access.getRoot(file.getAbsolutePath());
//				if(root == null){
//					throw new IllegalArgumentException(MessageFormat.format(Messages.getString("RemoteRun.passed.path.is.not.shared.error.message"), file.getAbsolutePath())); //$NON-NLS-1$
//				}
//				LOGGER.debug(MessageFormat.format("Share \"{0}\" found for Remote Run", root)); //$NON-NLS-1$
//				ArrayList<File> rootFiles = result.get(root);
//				if(rootFiles == null){
//					rootFiles = new ArrayList<File>();
//					result.put(root, rootFiles);
//				}
//				rootFiles.add(file);
//			}
//			return result;
//			
//		} catch (IllegalArgumentException e){
//			//perhaps there is no shares. discard collected and create virtual share by passed configuration
//			LOGGER.debug("Error got during Share association. Will not use any persisted Sahre's Info for VcsRoot binding", e); //$NON-NLS-1$
//			return Collections.singletonMap(createInplaceShare(server, cfgId), new ArrayList<File>(files));
//		}
//		
//	}
//
//	@Deprecated
//	private IShare createInplaceShare(final Server server, final String cfgId) throws IllegalArgumentException, ECommunicationException {
//		//check Configuration is defined
//		if(cfgId == null){
//			throw new IllegalArgumentException(MessageFormat.format(Messages.getString("RemoteRun.configuration.omitted.with.unshared.folder.error.pattern"), "?"));			 //$NON-NLS-1$
//		}
//		//seek Configuration
//		final BuildTypeData configuration = server.getConfiguration(cfgId);
//		if(configuration == null){
//			throw new IllegalArgumentException(MessageFormat.format(Messages.getString("RemoteRun.wrong.configuration.id.error.pattern"), cfgId)); //$NON-NLS-1$
//		}
//		//get single VcsRoot
//		final List<? extends VcsRoot> roots = configuration.getVcsRoots();
//		if(roots.isEmpty()){
//			throw new IllegalArgumentException(MessageFormat.format("Could not get Default VcsRoot in Configuration \"{0}\": no one VcsRoot attached.", cfgId)); //$NON-NLS-1$
//			
//		} else if(roots.size() > 1){
//			throw new IllegalArgumentException(MessageFormat.format(Messages.getString("RemoteRun.no.default.vcsroot.error.pattern"), cfgId, roots.size())); //$NON-NLS-1$
//			
//		}
//		//construct virtual
//		final VcsRoot singleRoot = roots.get(0);
//		final IShare inplaceShare = new IShare(){
//			
//			@Override
//			public String toString() {
//				return MessageFormat.format("local={0}, remote={1}, vcs={2}, properties={3}",  //$NON-NLS-1$
//						getLocal(), getRemote(), getVcs(), getProperties());
//			}
//
//			public String getId() {
//				return String.valueOf(System.currentTimeMillis());
//			}
//
//			public String getLocal() {
//				//let's use current directory
//				return System.getProperty("user.dir"); //$NON-NLS-1$
//			}
//
//			public Map<String, String> getProperties() {
//				return singleRoot.getProperties();
//			}
//
//			public Long getRemote() {
//				return singleRoot.getId();
//			}
//
//			public String getVcs() {
//				return singleRoot.getVcsName();
//			}
//		};
//		LOGGER.debug(MessageFormat.format("Virtual Share was created: {0}", inplaceShare)); //$NON-NLS-1$
//		return inplaceShare;
//	}

	Collection<File> getFiles(Args args, IProgressMonitor monitor) throws IllegalArgumentException {
		monitor.beginTask(Messages.getString("RemoteRun.collect.changes.step.name")); //$NON-NLS-1$
		final String[] elements = args.getArguments();
		int i = 0;//skip command
		while (i < elements.length) {
			final String currentToken = elements[i].toLowerCase();
			if(elements[i].startsWith("-")){ //$NON-NLS-1$
				if(elements[i].toLowerCase().equals(NO_WAIT_SWITCH) 
						|| currentToken.equals(NO_WAIT_SWITCH_LONG) 
						|| currentToken.equals(NO_USE_SHARES_SWITCH_LONG)){
					i++; //single token
				} else {
					i++; //arg
					i++; //args value
				}
			} else {
				//reach files
				break;
			}
		}
		
		final HashSet<File> result = new HashSet<File>();
		if (elements.length > i) {//file's part existing
			for (; i < elements.length; i++) {
				final String path = elements[i];
				final Collection<File> files;
				if (!path.startsWith("@")) { //$NON-NLS-1$
					files = Util.getFiles(path);
				} else {
					files = Util.getFiles(new File(path.substring(1, path.length())));
				}
				// filter out system files
				result.addAll(TCC_FILTER.accept(Util.SVN_FILES_FILTER.accept(Util.CVS_FILES_FILTER.accept(files))));
			}
		} else {//let's use current directory as root if nothing passed
			result.addAll(TCC_FILTER.accept(Util.SVN_FILES_FILTER.accept(Util.CVS_FILES_FILTER.accept(Util.getFiles("."))))); //$NON-NLS-1$
		}

		if (result.size() == 0) {
			throw new IllegalArgumentException(Messages.getString("RemoteRun.no.files.collected.for.remoterun.eror.message")); //$NON-NLS-1$
		}
		monitor.done(MessageFormat.format(Messages.getString("RemoteRun.collect.changes.step.result.pattern"), result.size())); //$NON-NLS-1$
		return result;
	}


	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired(final Args args) {
		return true;
	}

	public String getUsageDescription() {
		return MessageFormat.format(Messages.getString("RemoteRun.help.usage.pattern"),  //$NON-NLS-1$
				getCommandDescription(),
				getId(), CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG, 
				MESSAGE_PARAM, MESSAGE_PARAM_LONG, 
				TIMEOUT_PARAM, TIMEOUT_PARAM_LONG,
				NO_WAIT_SWITCH, NO_WAIT_SWITCH_LONG,
				CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG,
				MESSAGE_PARAM, MESSAGE_PARAM_LONG,
				TIMEOUT_PARAM, TIMEOUT_PARAM_LONG,
				NO_WAIT_SWITCH, NO_WAIT_SWITCH_LONG
				);
	}
	
	public String getCommandDescription() {
		return Messages.getString("RemoteRun.help.description"); //$NON-NLS-1$
	}


	public String getResultDescription() {
		return myResultDescription;
	}

	public void validate(Args args) throws IllegalArgumentException {
		if(args == null || !args.hasArgument(MESSAGE_PARAM, MESSAGE_PARAM_LONG)){
			throw new IllegalArgumentException(MessageFormat.format(Messages.getString("RemoteRun.missing.message.para.error.pattern"), MESSAGE_PARAM, MESSAGE_PARAM_LONG));		 //$NON-NLS-1$
		}
	}

}

package com.jetbrains.teamcity.commandline;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

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

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Logger;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.URLFactory;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.resources.IShare;
import com.jetbrains.teamcity.resources.TCAccess;

public class RemoteRun implements ICommand {

	private static final int SLEEP_INTERVAL = 1000 * 10;
	private static final int DEFAULT_TIMEOUT = 1000 * 60 * 60;

	private static final String ID = "remoterun";
	
	private static final String CONFIGURATION_PARAM = "-c";
	private static final String CONFIGURATION_PARAM_LONG = "--configuration";
	
	private static final String MESSAGE_PARAM = "-m";
	private static final String MESSAGE_PARAM_LONG = "--message";
	
	private static final String TIMEOUT_PARAM = "-t";
	private static final String TIMEOUT_PARAM_LONG = "--timeout";
	
	private static final String NO_WAIT_SWITCH = "-n";
	private static final String NO_WAIT_SWITCH_LONG = "--nowait";
	
	private static final String PATCHES_FOLDER = ".";
	
	private String myConfigurationId;
	private Collection<File> myFiles;
	private HashMap<IShare, ArrayList<File>> myRootMap;
	private Server myServer;
	private String myComments = "<no comments>";
	private boolean isNoWait = false;
	private long myTimeout;

	public void execute(final Server server, String[] args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(Util.hasArgument(args, CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG) ){
			myServer = server;
			//configuration
			myConfigurationId = Util.getArgumentValue(args, CONFIGURATION_PARAM, CONFIGURATION_PARAM_LONG);
			//comment
			if(Util.hasArgument(args, MESSAGE_PARAM, MESSAGE_PARAM_LONG)){
				myComments = Util.getArgumentValue(args, MESSAGE_PARAM, MESSAGE_PARAM_LONG);
			}
			//wait/no wait for build result
			if(Util.hasArgument(args, NO_WAIT_SWITCH, NO_WAIT_SWITCH_LONG)){
				isNoWait  = true;
			}
			//timeout
			if(Util.hasArgument(args, TIMEOUT_PARAM, TIMEOUT_PARAM_LONG)){
				myTimeout = Long.valueOf(Util.getArgumentValue(args, TIMEOUT_PARAM, TIMEOUT_PARAM_LONG));
			} else {
				myTimeout = DEFAULT_TIMEOUT;
			}
			//lets go...
			myFiles = getFiles(args);
			myRootMap = getRootMap(myFiles);
			//start RR
			final long chaneListId = fireRemoteRun(myRootMap);
			//process result
			if(!isNoWait){
				waitForResult(chaneListId, myTimeout);
			}
			return;
		}
		System.out.println(getUsageDescription());
	}


	private PersonalChangeDescriptor waitForResult(final long changeListId, final long timeOut) throws ECommunicationException, ERemoteError {
		final long startTime = System.currentTimeMillis();
		while ((System.currentTimeMillis() - startTime) < timeOut) {
			final TeamServerSummaryData summary = myServer.getSummary();
			for(final UserChangeInfoData data : summary.getPersonalChanges()){
				if(data.getPersonalDesc() != null && data.getPersonalDesc().getId() == changeListId){
					//check builds status 
					if (UserChangeStatus.FAILED == data.getChangeStatus() 
							|| UserChangeStatus.CANCELED == data.getChangeStatus()) {
						throw new ERemoteError(MessageFormat.format("RemoteRun failed: build status={0}", data.getChangeStatus()));
					}
					final PersonalChangeDescriptor descriptor = data.getPersonalDesc();
					PersonalChangeCommitDecision commitStatus = descriptor.getPersonalChangeStatus();
					if(PersonalChangeCommitDecision.COMMIT == commitStatus){
						return descriptor;
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
		throw new RuntimeException(MessageFormat.format("RemoteRun failed: timeout {0} ms exceed", myTimeout));
	}


	private long fireRemoteRun(final HashMap<IShare, ArrayList<File>> map) throws ECommunicationException, ERemoteError {
		final int currentUser = myServer.getCurrentUser();
		try {
			//prepare change list & patch for remote run
			final long changeId = createChangeList(myServer.getURL(), currentUser, myConfigurationId, myRootMap);
			//schedule for execution and process status
			final ArrayList<AddToQueueRequest> batch = new ArrayList<AddToQueueRequest>();
			final AddToQueueRequest request = new AddToQueueRequest(myConfigurationId, changeId);
			batch.add(request);
			final AddToQueueResult result = myServer.addRemoteRunToQueue(batch, myComments);//TODO: process Result here
			if(result.hasFailures()){
				throw new ERemoteError(result.getFailureReason(myConfigurationId));
			}
			return changeId;
		} catch (IOException e) {
			throw new ECommunicationException(e);
		}
		
	}
	
	
	private long createChangeList(String serverURL, int userId, String configuration, HashMap<IShare,ArrayList<File>> map) throws IOException, ECommunicationException {
		
		final File patch = createPatch(createPatchFile(serverURL), map);
		patch.deleteOnExit();

		final SimpleHttpConnectionManager manager = new SimpleHttpConnectionManager();
		HostConfiguration hostConfiguration = new HostConfiguration();
		hostConfiguration.setHost(new URI(serverURL, false));
		HttpConnection connection = manager.getConnection(hostConfiguration);

		if (!connection.isOpen()) {
			connection.open();
		}
		String _uri = serverURL;
		if (!serverURL.endsWith("/")) {
			_uri += "/";
		}
		_uri += "uploadChanges.html";
		final PostMethod postMethod = new PostMethod(_uri);
		final BufferedInputStream content = new BufferedInputStream(new FileInputStream(patch));
		try {
			postMethod.setRequestEntity(new InputStreamRequestEntity(content, patch.length()));
			postMethod.setQueryString(new NameValuePair[] {
					new NameValuePair("userId", String.valueOf(userId)),
					new NameValuePair("description", myComments),
					new NameValuePair("date", String.valueOf(System.currentTimeMillis())),
					new NameValuePair("commitType", String.valueOf(PreTestedCommitType.COMMIT_IF_SUCCESSFUL.getId())), });//TODO: make argument
			postMethod.execute(new HttpState(), connection);
		} finally {
			content.close();
		}
		//post requests to queue
		final String response = postMethod.getResponseBodyAsString();
		return Long.parseLong(response);
	}


	private File createPatch(File patchFile, final HashMap<IShare, ArrayList<File>> map) throws IOException, ECommunicationException {
		DataOutputStream os = null;
		LowLevelPatchBuilderImpl patcher = null;
		try{
			os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(patchFile)));
			patcher = new LowLevelPatchBuilderImpl(os);
			//
			for(final Entry<IShare, ArrayList<File>> entry : map.entrySet()){
				final URLFactory factory = URLFactory.getFactory(entry.getKey()); 
				for(final File file : entry.getValue()){
					LowLevelPatchBuilder.WriteFileContent content = new PatchBuilderImpl.StreamWriteFileContent(new BufferedInputStream(new FileInputStream(file)), file.length());
					final String fileUrl = factory.getUrl(file);
					if(file.exists()){
						patcher.changeBinary(fileUrl, (int) file.length(), content, false);
					} else {
						patcher.delete(fileUrl, true, false);
					}
				}
			}
			//
		} finally{
			patcher.exit("");
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

	private static File createPatchFile(String url) {
		File stateLocation = new File(PATCHES_FOLDER);
		try {
			url = new String(Base64.encodeBase64(MessageDigest.getInstance("MD5").digest(url.getBytes())));
		} catch (NoSuchAlgorithmException e) {
			//
		}
		File file = new File(stateLocation, url + ".patch");
		file.getParentFile().mkdirs();
		return file;
	}

	private HashMap<IShare, ArrayList<File>> getRootMap(final Collection<File> files) throws IllegalArgumentException {
		final HashMap<IShare, ArrayList<File>> result = new HashMap<IShare, ArrayList<File>>();
		for(final File file : files){
			final IShare root = TCAccess.getInstance().getRoot(file.getAbsolutePath());
			if(root == null){
				throw new IllegalArgumentException(MessageFormat.format("Path is not shared: {0}", file.getAbsolutePath()));
			}
			ArrayList<File> rootFiles = result.get(root);
			if(rootFiles == null){
				rootFiles = new ArrayList<File>();
				result.put(root, rootFiles);
			}
			rootFiles.add(file);
		}
		return result;
	}

	private Collection<File> getFiles(String[] args) throws IllegalArgumentException {
		int i = 1;//skip command
		while (i < args.length) {
			final String currentToken = args[i].toLowerCase();
			if(args[i].startsWith("-")){
				if(args[i].toLowerCase().equals(NO_WAIT_SWITCH) || currentToken.equals(NO_WAIT_SWITCH_LONG)){
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
		for (; i < args.length; i++) {
			final String path = args[i];
			final Collection<File> files;
			if(!path.startsWith("@")){
				files = Util.getFiles(path);
			} else {
				files = Util.getFiles(new File(path.substring(1, path.length())));
			}
			//filter out system files 
			result.addAll(Util.SVN_FILES_FILTER.accept(Util.CVS_FILES_FILTER.accept(files)));
		}
		Logger.log(RemoteRun.class.getName(), MessageFormat.format("Collected {0} files for Remote Run", result.size()));
		return result;
	}


	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired() {
		return true;
	}

	public String getUsageDescription() {
		return MessageFormat.format("{0}: use -c|--configuration [configuration_id] -m|--message [message] -t|--timeout [timeout,sec] [-n|--nowait] file [file ...]| @filelist", getId()); 
	}
	
	public String getDescription() {
		return "Fires Personal Build";
	}
	

}

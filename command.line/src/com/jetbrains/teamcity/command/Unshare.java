package com.jetbrains.teamcity.command;

import java.text.MessageFormat;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.resources.TCAccess;

public class Unshare implements ICommand {

	private static final String SHARE_PARAM = "-s";
	private static final String SHARE_PARAM_LONG = "--share";
	private static final String ID = "unshare";

	public void execute(final Server server, Args args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if (args.hasArgument(SHARE_PARAM, SHARE_PARAM_LONG)) {
			final String shareId = args.getArgument(SHARE_PARAM, SHARE_PARAM_LONG);
			TCAccess.getInstance().unshare(shareId);
			System.out.println("SUCCESS");
			return;
		}
		System.out.println(getUsageDescription());
	}

	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired(final Args args) {
		return false;
	}

	public String getUsageDescription() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getDescription()).append("\n");
		buffer.append(MessageFormat.format("usage: {0} {1}[{2}] ARG_SHAREID", 
				getId(), SHARE_PARAM, SHARE_PARAM_LONG)).append("\n");
		buffer.append("\n");
		buffer.append("Valid options:").append("\n");;
		buffer.append(MessageFormat.format("\t{0}[{1}] ARG_SHAREID\t: {2}", SHARE_PARAM, SHARE_PARAM_LONG, "share's id that asked for removing. Can be found using by \"share --info\" command")).append("\n");
		return buffer.toString();
		
	}
	
	public String getDescription() {
		return "Remove association of local folder with TeamCity VcsRoot";
	}
	

}

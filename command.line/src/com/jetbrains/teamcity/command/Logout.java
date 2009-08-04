package com.jetbrains.teamcity.command;

import java.text.MessageFormat;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.resources.TCAccess;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

public class Logout implements ICommand {

	public void execute(Server server, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(args.hasArgument(Login.HOST_PARAM, Login.HOST_PARAM_LONG)){
			final String url = args.getArgument(Login.HOST_PARAM, Login.HOST_PARAM_LONG);
			TCAccess.getInstance().removeCredential(url);
			System.out.println("SUCCESS");
			return;
		}
		System.out.println(getUsageDescription());
	}

	public String getDescription() {
		return "Remove credential for TeamCity server";
	}

	public String getId() {
		return "logout";
	}

	public String getUsageDescription() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getDescription()).append("\n");
		buffer.append(MessageFormat.format("usage: {0} {1}[{2}] ARG_HOST", getId(), Login.HOST_PARAM, Login.HOST_PARAM_LONG)).append("\n");
		buffer.append("\n");
		return buffer.toString();
	}

	public boolean isConnectionRequired(final Args args) {
		return false;
	}

}

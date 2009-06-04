package com.jetbrains.teamcity.commandline;

import java.text.MessageFormat;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.resources.TCAccess;

public class Logout implements ICommand {

	public void execute(Server server, String[] args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(Util.hasArgument(args, Login.HOST_PARAM, Login.HOST_PARAM_LONG)){
			final String url = Util.getArgumentValue(args, Login.HOST_PARAM, Login.HOST_PARAM_LONG);
			TCAccess.getInstance().removeCredential(url);
			return;
		}
		System.out.println(getUsageDescription());
	}

	public String getDescription() {
		return "Removes credential for TeamCity server";
	}

	public String getId() {
		return "logout";
	}

	public String getUsageDescription() {
		return MessageFormat.format("{0} --host \"url\"", getId());
	}

	public boolean isConnectionRequired() {
		return false;
	}

}

package com.jetbrains.teamcity.commandline;

import java.text.MessageFormat;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.resources.VCSAccess;

public class Logout implements ICommand {

	@Override
	public void execute(Server server, String[] args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(Util.hasArgument(args, Login.HOST_PARAM, Login.HOST_PARAM_LONG)){
			final String url = Util.getArgumentValue(args, Login.HOST_PARAM, Login.HOST_PARAM_LONG);
			VCSAccess.getInstance().removeCredential(url);
			return;
		}
		System.out.println(getUsageDescription());
	}

	@Override
	public String getDescription() {
		return "Removes credential for TeamCity server";
	}

	@Override
	public String getId() {
		return "logout";
	}

	@Override
	public String getUsageDescription() {
		return MessageFormat.format("{0} --host \"url\"", getId());
	}

	@Override
	public boolean isConnectionRequired() {
		return false;
	}

}

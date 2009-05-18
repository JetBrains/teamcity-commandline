package com.jetbrains.teamcity.commandline;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;

public class TestCommand implements ICommand {

	@Override
	public void execute(Server server, String[] args)
			throws EAuthorizationException, ECommunicationException,
			ERemoteError, InvalidAttributesException {
		// TODO Auto-generated method stub
	}

	@Override
	public String getHelp() {
		return "";
	}

	@Override
	public String getId() {
		return "testId";
	}

	@Override
	public boolean isConnectionRequired() {
		return false;
	}

}

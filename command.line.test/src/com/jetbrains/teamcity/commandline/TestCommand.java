package com.jetbrains.teamcity.commandline;

import javax.naming.directory.InvalidAttributesException;

import org.junit.Ignore;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;

@Ignore
public class TestCommand implements ICommand {

	@Override
	public void execute(Server server, String[] args)
			throws EAuthorizationException, ECommunicationException,
			ERemoteError, InvalidAttributesException {
		// TODO Auto-generated method stub
	}

	@Override
	public String getUsageDescription() {
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

	@Override
	public String getDescription() {
		return "Test command";
	}

}

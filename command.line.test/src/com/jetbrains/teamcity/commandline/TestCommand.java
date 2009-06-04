package com.jetbrains.teamcity.commandline;

import javax.naming.directory.InvalidAttributesException;

import org.junit.Ignore;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;

@Ignore
public class TestCommand implements ICommand {

	public void execute(Server server, String[] args)
			throws EAuthorizationException, ECommunicationException,
			ERemoteError, InvalidAttributesException {
	}

	public String getUsageDescription() {
		return "";
	}

	public String getId() {
		return "testId";
	}

	public boolean isConnectionRequired() {
		return false;
	}

	public String getDescription() {
		return "Test command";
	}

}

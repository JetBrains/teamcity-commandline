package com.jetbrains.teamcity.command;

import javax.naming.directory.InvalidAttributesException;

import org.junit.Ignore;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

@Ignore
public class TestCommand implements ICommand {

	public void execute(Server server, Args args, final IProgressMonitor monitor)
			throws EAuthorizationException, ECommunicationException,
			ERemoteError, InvalidAttributesException {
	}

	public String getUsageDescription() {
		return "";
	}

	public String getId() {
		return "testId";
	}

	public boolean isConnectionRequired(Args args) {
		return false;
	}

	public String getCommandDescription() {
		return "Test command";
	}

	public String getResultDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public void validate(Args args) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}

}

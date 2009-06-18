package com.jetbrains.teamcity.command;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;

public interface ICommand {
	
	public String getId();
	
	public void execute(final Server server, final Args args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException ;

	public boolean isConnectionRequired(final Args args);
	
	public String getUsageDescription();

	public String getDescription();
	
}

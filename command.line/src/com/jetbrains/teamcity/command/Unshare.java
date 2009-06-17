package com.jetbrains.teamcity.command;

import java.text.MessageFormat;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.command.ICommand;
import com.jetbrains.teamcity.resources.TCAccess;

public class Unshare implements ICommand {

	private static final String ID = "unshare";

	public void execute(final Server server, String[] args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if (Util.hasArgument(args, "-s", "--share")) {
			final String shareId = Util.getArgumentValue(args, "-s", "--share");
			TCAccess.getInstance().unshare(shareId);
			System.out.println("SUCCESS");
			return;
		}
		System.out.println(getUsageDescription());
	}

	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired() {
		return false;
	}

	public String getUsageDescription() {
		return MessageFormat.format("{0}: use -s|--share [share_id]", getId()); 
	}
	
	public String getDescription() {
		return "Removes association of local folder with TeamCity VcsRoot";
	}
	

}

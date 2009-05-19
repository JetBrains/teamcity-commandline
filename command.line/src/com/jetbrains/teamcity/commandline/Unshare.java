package com.jetbrains.teamcity.commandline;

import java.text.MessageFormat;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.resources.VCSAccess;

public class Unshare implements ICommand {

	private static final String ID = "unshare";

	@Override
	public void execute(final Server server, String[] args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if (Util.hasArgument(args, "-s", "--share")) {
			final String shareId = Util.getArgumentValue(args, "-s", "--share");
			VCSAccess.getInstance().unshare(shareId);
			return;
		}
		System.out.println(getUsageDescription());
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public boolean isConnectionRequired() {
		return false;
	}

	@Override
	public String getUsageDescription() {
		return MessageFormat.format("{0}: use -s|--share [share_id]", getId()); 
	}
	
	@Override
	public String getDescription() {
		return "Removes association of local folder with TeamCity VcsRoot";
	}
	

}

package com.jetbrains.teamcity.command;

import java.text.MessageFormat;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.resources.TCAccess;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

public class Unshare implements ICommand {

	private static final String ID = Messages.getString("Unshare.command.id"); //$NON-NLS-1$
	
	private String myResultDescription;
	
	public void validate(Args args) throws IllegalArgumentException {
		if(args.getLastArgument() == null){
			throw new IllegalArgumentException("missing <shareId>");
		}
	}

	public void execute(final Server server, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		final String shareId = args.getLastArgument();
		TCAccess.getInstance().unshare(shareId);
		myResultDescription = MessageFormat.format(Messages.getString("Unshare.result.ok.pattern"), shareId); //$NON-NLS-1$
		return;
	}

	public String getId() { 
		return ID;
	}

	public boolean isConnectionRequired(final Args args) {
		return false;
	}

	public String getUsageDescription() {
		return MessageFormat.format(Messages.getString("Unshare.help.usage.pattern"), getCommandDescription(), getId());//$NON-NLS-1$
	}
	
	public String getCommandDescription() {
		return Messages.getString("Unshare.help.description"); //$NON-NLS-1$
	}
	
	public String getResultDescription() {
		return myResultDescription;
	}
	
	
	

}

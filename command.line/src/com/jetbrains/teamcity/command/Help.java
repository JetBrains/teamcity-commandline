package com.jetbrains.teamcity.command;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.TreeSet;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.Build;
import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

class Help implements ICommand {

	static final String ID = Messages.getString("Help.command.id"); //$NON-NLS-1$
	private String myResultDescription;
	
	public void execute(Server server, final Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		
		final StringBuffer buffer = new StringBuffer();
		
		buffer.append(MessageFormat.format(Messages.getString("Help.command.header"),  //$NON-NLS-1$
				Build.major, Build.build));
		
		//print delail help
		if (args.getCommandId() != null && args.getCommandId() != ID/*do not include auto inserted help*/) {
			//prints detail help for command(arg[0])
			buffer.append(printDescription(args.getCommandId()));
			myResultDescription = buffer.toString();
			return;
		} else {
			buffer.append(printDefault());
			myResultDescription = buffer.toString();
			return; 
		}
		
	}

	private String printDefault() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append(Messages.getString("Help.tool.usage.description")); //$NON-NLS-1$
		//print command list
		buffer.append(Messages.getString("Help.available.commands.list.header")); //$NON-NLS-1$
		final TreeSet<ICommand> knownCommands = new TreeSet<ICommand>(new Comparator<ICommand>(){
			public int compare(ICommand o1, ICommand o2) {
				return o1.getId().compareTo(o2.getId());
			}});

		knownCommands.addAll(CommandRegistry.getInstance().commands());
		for(final ICommand command : knownCommands){
			buffer.append(MessageFormat.format(Messages.getString("Help.available.commands.list.pattern"), String.valueOf(command.getId()), String.valueOf(command.getCommandDescription()))); //$NON-NLS-1$
		}
		buffer.append(Messages.getString("Help.command.usage.text")); //$NON-NLS-1$
		buffer.append(printGlobalOptions());
		return buffer.toString();
	}

	private String printGlobalOptions() {
		final String globalOptions = Messages.getString("Help.global.options.header"); //$NON-NLS-1$
		return MessageFormat.format(globalOptions, CommandRunner.HOST_ARG, CommandRunner.USER_ARG, CommandRunner.PASSWORD_ARG);
	}

	private String printDescription(final String commandId) {
		StringBuffer buffer = new StringBuffer();
		final ICommand command = CommandRegistry.getInstance().getCommand(commandId);
		if (command != null && command.getId().equals(commandId)) {
			buffer.append(command.getUsageDescription());
			buffer.append(printGlobalOptions());
			return buffer.toString();
		} else {
			buffer.append(MessageFormat.format(Messages.getString("Help.no.one.registered.command.found.message"), commandId)); //$NON-NLS-1$
			buffer.append(printDefault());
			return buffer.toString();
		}
	}

	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired(final Args args) {
		return false;
	}

	public String getUsageDescription() {
		return getCommandDescription();
	}

	public String getCommandDescription() {
		return Messages.getString("Help.command.description"); //$NON-NLS-1$
	}
	
	public String getResultDescription() {
		return myResultDescription;
	}

	public void validate(Args args) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}
	
	

}

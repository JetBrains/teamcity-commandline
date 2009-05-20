package com.jetbrains.teamcity.commandline;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.TreeSet;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;

class Help implements ICommand {

	static final String ID = "help";
	
	private static final String HELP_PARAM = "-h";
	private static final String HELP_PARAM_LONG = "--help";

	@Override
	public void execute(Server server, String[] args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		System.out.println("TeamCity Commandline tool v0.0.0.1. Copyright 2000-2009 JetBrains s.r.o.");
		if(Util.hasArgument(args, HELP_PARAM, HELP_PARAM_LONG) ){
			final String commandId = Util.getArgumentValue(args, HELP_PARAM, HELP_PARAM_LONG);
			final ICommand command = CommandRegistry.getInstance().getCommand(commandId);
			if(command != null){
				System.out.println(command.getUsageDescription());
				return;
			}
		}
		
		System.out.println("Available commands:");
		final TreeSet<ICommand> knownCommands = new TreeSet<ICommand>(new Comparator<ICommand>(){
			@Override
			public int compare(ICommand o1, ICommand o2) {
				return o1.getId().compareTo(o2.getId());
			}});
		knownCommands.addAll(CommandRegistry.getInstance().commands());
		for(final ICommand command : knownCommands){
			System.out.println(MessageFormat.format("\t{0}\t\t{1}", String.valueOf(command.getId()), String.valueOf(command.getDescription())));
		}
		System.out.println();
		System.out.println("use -h|--help [command] for command usage information");
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
		return null;
	}

	@Override
	public String getDescription() {
		return "Prints this screen";
	}

}

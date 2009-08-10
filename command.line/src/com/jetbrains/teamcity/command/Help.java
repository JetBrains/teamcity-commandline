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

	static final String ID = "help";
	
	public void execute(Server server, final Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		
		System.out.println(MessageFormat.format("TeamCity Commandline utility {0}. Copyright 2000-2009 JetBrains s.r.o.", Build.number));
		
		final String[] elements = args.getArguments();
		//print delail help
		if (elements.length > 0 && elements[0] != ID/*do not include auto inserted help*/) {
			printDescription(elements[0]);
			return;
		}
		//print command list
		System.out.println("Available commands:");
		final TreeSet<ICommand> knownCommands = new TreeSet<ICommand>(new Comparator<ICommand>(){
			public int compare(ICommand o1, ICommand o2) {
				return o1.getId().compareTo(o2.getId());
			}});
		
		knownCommands.addAll(CommandRegistry.getInstance().commands());
		for(final ICommand command : knownCommands){
			System.out.println(MessageFormat.format("\t{0}\t\t{1}", String.valueOf(command.getId()), String.valueOf(command.getCommandDescription())));
		}
		System.out.println();
		System.out.println("use help [command] for command's usage information");
		
		printGlobalOptions();
		
	}

	private void printGlobalOptions() {
		final String globalOptions = "Global options:\n\t{0} ARG\t: specify a host ARG\n\t{1} ARG\t: specify a username ARG\n\t{2} ARG\t: specify a password ARG";
		System.out.println(MessageFormat.format(globalOptions, CommandRunner.HOST_ARG, CommandRunner.USER_ARG, CommandRunner.PASSWORD_ARG));
	}

	private void printDescription(final String commandId) {
		final ICommand command = CommandRegistry.getInstance().getCommand(commandId);
		if (command != null && command.getId().equals(commandId)) {
			System.out.println(command.getUsageDescription());
			printGlobalOptions();
		} else {
			System.out.println(MessageFormat.format("No \"{0}\" command found", commandId));
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
		return "Display this screen";
	}
	
	public String getResultDescription() {
		return null;
	}
	
	

}

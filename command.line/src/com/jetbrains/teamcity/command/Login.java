package com.jetbrains.teamcity.command;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Scanner;

import javax.naming.directory.InvalidAttributesException;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.resources.TCAccess;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

public class Login implements ICommand {
	
	private static final String ID = Messages.getString("Login.command.id"); //$NON-NLS-1$
	
	private String myResultDescription;
	
	public void execute(Server nullServer, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(args.hasArgument(CommandRunner.HOST_ARG)){
			final String url = args.getArgument(CommandRunner.HOST_ARG);
			final String user = getUser(args);
			final String password = getPassword(args);
			//try to login
			try {
				final Server server = new Server(new URL(url));
				server.connect();
				server.logon(user, password);
				//ok. let's store
				TCAccess.getInstance().setCredential(url, user, password);
				myResultDescription = MessageFormat.format(Messages.getString("Login.result.ok.pattern"), user); //$NON-NLS-1$
				
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			} 
		} else {
			myResultDescription = getUsageDescription();
		}
	}
	
	private String getPassword(Args args) {
		if (args.hasArgument(CommandRunner.PASSWORD_ARG)) {
			return args.getArgument(CommandRunner.PASSWORD_ARG);
		} else {
			return readLine(Messages.getString("Login.password.prompt")); //$NON-NLS-1$
		}
	}

	private String getUser(Args args) {
		if (args.hasArgument(CommandRunner.USER_ARG)) {
			return args.getArgument(CommandRunner.USER_ARG);
		} else {
			return readLine(Messages.getString("Login.username.prompt")); //$NON-NLS-1$
		}
	}

	private String readLine(String prompth) {
		System.out.print(prompth);
		final Scanner scanner = new Scanner(System.in);
		return scanner.nextLine();
	}

	public String getCommandDescription() {
		return Messages.getString("Login.help.description"); //$NON-NLS-1$
	}

	public String getId() {
		return ID;
	}

	public String getUsageDescription() {
		return MessageFormat.format(Messages.getString("Login.help.usage.pattern"),  //$NON-NLS-1$
				getCommandDescription(), getId(), CommandRunner.HOST_ARG, CommandRunner.USER_ARG, CommandRunner.PASSWORD_ARG);
	}
	

	public boolean isConnectionRequired(final Args args) {
		return false;
	}
	
	public String getResultDescription() {
		return myResultDescription;
	}
	
	

}

package com.jetbrains.teamcity.command;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.resources.ICredential;
import com.jetbrains.teamcity.resources.TCAccess;

public class CommandRunner {
	
	static final String USER_ARG = "--user";
	static final String PASSWORD_ARG = "--password";
	static final String HOST_ARG = "--host";

	public static void main(final String[] args) throws Exception {
		
		final Args arguments = new Args(args);
		
		final ICommand command = CommandRegistry.getInstance().getCommand(arguments.getCommandId());
		try{
			if(command.isConnectionRequired(arguments)){
				final Server server = openConnection(arguments);
				command.execute(server, arguments);
			} else {
				command.execute(null, arguments);	
			}
		} catch (Throwable e){
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}
	
	private static Server openConnection(final Args args) throws MalformedURLException, ECommunicationException, EAuthorizationException {
		final String host = args.getArgument(HOST_ARG);
		
		if(host != null){
			final String user;
			final String password;
			if(args.hasArgument(USER_ARG, PASSWORD_ARG)){
				user = args.getArgument(USER_ARG);
				password = args.getArgument(PASSWORD_ARG);
				
			} else {
				//try to load from saved
				final ICredential credential = TCAccess.getInstance().findCredential(host);
				if(credential != null){
					user = credential.getUser();
					password = credential.getPassword();
				} else {
					throw new EAuthorizationException(MessageFormat.format("You are currently not logged in to \"{0}\". Run \"login\" command or specify \"--user\" & \"--password\"", host));
				}
			}
			final Server server = new Server(new URL(host));
			server.connect();
			server.logon(user, password);
			return server;
		} else {
			throw new IllegalArgumentException(MessageFormat.format("No host specified. Use \"{0} [url]\"", HOST_ARG));
		}
	}
	
}

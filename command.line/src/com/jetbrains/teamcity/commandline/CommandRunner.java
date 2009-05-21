package com.jetbrains.teamcity.commandline;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.Logger;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.resources.ICredential;
import com.jetbrains.teamcity.resources.VCSAccess;

public class CommandRunner {
	
	public static void main(final String[] args) throws Exception {
		
		String commandId = getCommandId(args);
		final ICommand command = CommandRegistry.getInstance().getCommand(commandId);
		try{
			if(command.isConnectionRequired()){
				final Server server = openConnection(args);
				command.execute(server, args);
			} else {
				command.execute(null, args);	
			}
		} catch (Throwable e){
			Logger.log(CommandRunner.class.getName(), e);
			System.exit(-1);
		}
	}
	
	private static Server openConnection(final String[] args) throws MalformedURLException, ECommunicationException, EAuthorizationException {
		final String host = Util.getArgumentValue(args, "--host");
		final String user;
		final String password;
		if(Util.hasArgument(args, "--user", "--password")){
			user = Util.getArgumentValue(args, "--user");
			password = Util.getArgumentValue(args, "--password");
		} else {
			//try to load from .tcpass
			final ICredential credential = VCSAccess.getInstance().findCredential(host);
			if(credential != null){
				user = credential.getUser();
				password = credential.getPassword();
			} else {
				throw new EAuthorizationException(MessageFormat.format("You are currently not logged in to \"{}\". Run \"login\" command or ise \"--user\" & \"--password\" switches", host));
			}
		}
		final Server server = new Server(new URL(host));
		server.connect();
		server.logon(user, password);
		return server;
	}


	private static String getCommandId(final String[] args) {
		if (args == null || args.length == 0) {
			return Help.ID;
		}
		return args[0].toLowerCase().trim();
	}
	
	
}

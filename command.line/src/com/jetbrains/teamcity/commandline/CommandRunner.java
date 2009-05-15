package com.jetbrains.teamcity.commandline;

import java.net.MalformedURLException;
import java.net.URL;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.Logger;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;

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
		Logger.log(CommandRunner.class.getName(), "Connectiong server...");
		final String host = Util.getArgumentValue(args, "--host");
		final String user = Util.getArgumentValue(args, "--user");
		final String password = Util.getArgumentValue(args, "--password");
		final Server server = new Server(new URL(host));
		server.connect();
		server.logon(user, password);
		Logger.log(CommandRunner.class.getName(), "Done.");
		return server;
	}


	private static String getCommandId(final String[] args) {
		if (args == null || args.length == 0) {
			return Help.ID;
		}
		return args[0].toLowerCase().trim();
	}
	
	
}

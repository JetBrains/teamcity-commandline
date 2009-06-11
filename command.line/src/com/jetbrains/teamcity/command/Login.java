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
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.command.ICommand;
import com.jetbrains.teamcity.resources.TCAccess;

public class Login implements ICommand {
	
	static final String HOST_PARAM = "-h";
	static final String HOST_PARAM_LONG = "--host";
	
	private static final String USER_PARAM = "-u";
	private static final String USER_PARAM_LONG = "--user";

	private static final String PASSWORD_PARAM = "-p";
	private static final String PASSWORD_PARAM_LONG = "--password";
	
	//	final String home = ;
	//	myStorageFile = home + File.separator + TC_STORAGE_DEFAULT_FILENAME;


	public void execute(Server nullServer, String[] args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(Util.hasArgument(args, HOST_PARAM, HOST_PARAM_LONG)){
			final String url = Util.getArgumentValue(args, HOST_PARAM, HOST_PARAM_LONG);
			final String user = getUser(args);
			final String password = getPassword(args);
			//try to login
			try {
				final Server server = new Server(new URL(url));
				server.connect();
				server.logon(user, password);
				//ok. let's store
				TCAccess.getInstance().setCredential(url, user, password);
				
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			} 
		} else {
			System.out.println(getUsageDescription());
		}
	}
	
	private String getPassword(String[] args) {
		if (Util.hasArgument(args, PASSWORD_PARAM, PASSWORD_PARAM_LONG)) {
			return Util.getArgumentValue(args, PASSWORD_PARAM, PASSWORD_PARAM_LONG);
		} else {
			return readLine("enter password:");
		}
	}

	private String getUser(String[] args) {
		if (Util.hasArgument(args, USER_PARAM, USER_PARAM_LONG)) {
			return Util.getArgumentValue(args, USER_PARAM, USER_PARAM_LONG);
		} else {
			return readLine("enter username:");
		}
	}

	private String readLine(String prompth) {
		System.out.print(prompth);
		final Scanner scanner = new Scanner(System.in);
		return scanner.nextLine();
	}

	public String getDescription() {
		return "Prompt for password for authenticating server";
	}

	public String getId() {
		return "login";
	}

	public String getUsageDescription() {
		return MessageFormat.format("{0} --host \"url\" [--user \"username\"] [--password \"password\"]", getId());
	}

	public boolean isConnectionRequired() {
		return false;
	}

}

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

public class Login implements ICommand {
	
	static final String HOST_PARAM = "-h";
	static final String HOST_PARAM_LONG = CommandRunner.HOST_ARG;
	
	private static final String USER_PARAM = "-u";
	private static final String USER_PARAM_LONG = CommandRunner.USER_ARG;

	private static final String PASSWORD_PARAM = "-p";
	private static final String PASSWORD_PARAM_LONG = CommandRunner.PASSWORD_ARG;
	
	public void execute(Server nullServer, Args args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(args.hasArgument(HOST_PARAM, HOST_PARAM_LONG)){
			final String url = args.getArgument(HOST_PARAM, HOST_PARAM_LONG);
			final String user = getUser(args);
			final String password = getPassword(args);
			//try to login
			try {
				final Server server = new Server(new URL(url));
				server.connect();
				server.logon(user, password);
				//ok. let's store
				TCAccess.getInstance().setCredential(url, user, password);
				System.out.println("SUCCESS");
				
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			} 
		} else {
			System.out.println(getUsageDescription());
		}
	}
	
	private String getPassword(Args args) {
		if (args.hasArgument(PASSWORD_PARAM, PASSWORD_PARAM_LONG)) {
			return args.getArgument(PASSWORD_PARAM, PASSWORD_PARAM_LONG);
		} else {
			return readLine("enter password:");
		}
	}

	private String getUser(Args args) {
		if (args.hasArgument(USER_PARAM, USER_PARAM_LONG)) {
			return args.getArgument(USER_PARAM, USER_PARAM_LONG);
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
		return "Prompt for username and password for authenticating TeamCity Server";
	}

	public String getId() {
		return "login";
	}

	public String getUsageDescription() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getDescription()).append("\n");
		buffer.append(MessageFormat.format("usage: {0} {1}[{2}] ARG_HOST [{3}[{4}] ARG_USERNAME {5}[{6}] ARG_PASSWORD]", getId(), HOST_PARAM, HOST_PARAM_LONG, USER_PARAM, USER_PARAM_LONG, PASSWORD_PARAM, PASSWORD_PARAM_LONG)).append("\n");
		buffer.append("\n");
		buffer.append("With no username or password args, prompt for input username and password interactive").append("\n");;
		return buffer.toString();
	}
	

	public boolean isConnectionRequired(final Args args) {
		return false;
	}

}

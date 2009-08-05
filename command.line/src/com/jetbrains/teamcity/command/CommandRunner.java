package com.jetbrains.teamcity.command;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import jetbrains.buildServer.serverSide.crypt.EncryptUtil;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.Logger;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.resources.ICredential;
import com.jetbrains.teamcity.resources.TCAccess;
import com.jetbrains.teamcity.runtime.ConsoleProgressMonitor;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

public class CommandRunner {
	
	static final String USER_ARG = "--user";
	static final String PASSWORD_ARG = "--password";
	static final String HOST_ARG = "--host";
	
	private static Comparator<ICredential> ourCredentialComaparator = new Comparator<ICredential>() {
		public int compare(ICredential o1, ICredential o2) {
			if (o1.getCreationTimestamp() > o2.getCreationTimestamp()) {
				return -1;
			} else if (o1.getCreationTimestamp() < o2.getCreationTimestamp()) {
				return 1;
			}
			return 0;
		}
	};

	public static void main(final String[] args) throws Exception {
		
		final Args arguments = new Args(args);
		
		final IProgressMonitor monitor = new ConsoleProgressMonitor(System.out);
		
		final ICommand command = CommandRegistry.getInstance().getCommand(arguments.getCommandId());
		try{
			if(command.isConnectionRequired(arguments)){
				final Server server = openConnection(arguments, monitor);
				command.execute(server, arguments, monitor);
			} else {
				command.execute(null, arguments, monitor);
			}
			//print success result
			reportResult(command, monitor);
		} catch (Throwable e){
			//print error result
			reportError(command, e, monitor);
			System.exit(-1);
		} 
	}
	
	private static void reportError(ICommand command, Throwable e, IProgressMonitor monitor) {
		System.err.println(e.getMessage());
	}

	private static void reportResult(ICommand command, IProgressMonitor monitor) {
		if (command.getResultDescription() != null && command.getResultDescription().trim().length() != 0) {
			System.out.println(command.getResultDescription());
		}
	}

	private static Server openConnection(final Args args, final IProgressMonitor monitor) throws MalformedURLException, ECommunicationException, EAuthorizationException {
		String host = args.getArgument(HOST_ARG);
		//load default(any) if omitted
		if(host == null){
			host = getDefaultHost();
		}
		if(host != null){
			String user;
			String password;
			if(args.hasArgument(USER_ARG, PASSWORD_ARG)){
				user = args.getArgument(USER_ARG);
				password = args.getArgument(PASSWORD_ARG);
				
			} else {
				//try to load from saved
				final ICredential credential = TCAccess.getInstance().findCredential(host);
				if(credential != null){
					user = credential.getUser();
					try{
						password = EncryptUtil.unscramble(credential.getPassword());
					} catch (Throwable t){
						//the EncryptUtil raises exception if decoding string was not scrambled
						password = credential.getPassword();
					}
				} else {
					throw new EAuthorizationException(MessageFormat.format("You are currently not logged in to \"{0}\". Run \"login\" command or specify \"--user\" & \"--password\"", host));
				}
			}
			final Server server = new Server(new URL(host));
			monitor.beginTask(MessageFormat.format("Connecting to \"{0}\" TeamCity Server", host));
			server.connect();
			monitor.done();
			
			monitor.beginTask("Logging in");
			server.logon(user, password);
			monitor.done();
			return server;
		} else {
			throw new IllegalArgumentException(MessageFormat.format("No Default host and no host specified. Use \"{0} [url]\"", HOST_ARG));
		}
	}

	private static String getDefaultHost() {
		final Collection<ICredential> credentials = TCAccess.getInstance().credentials();
		if(!credentials.isEmpty()){
			//sort by creation TS. the newest will be used as default 
			final ArrayList<ICredential> ordered = new ArrayList<ICredential>(credentials);
			Collections.sort(ordered, ourCredentialComaparator);
			final ICredential defaultCredential = ordered.iterator().next();
			return defaultCredential.getServer();
		}
		return null;
	}
	
}

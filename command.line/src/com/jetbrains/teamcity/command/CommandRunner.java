package com.jetbrains.teamcity.command;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

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
		} catch (Throwable e){
			System.err.println(e.getMessage());
			System.exit(-1);
		} finally {
			monitor.done("");
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
			monitor.beginTask("Connecting to TeamCity Server", -1);
			server.connect();
			monitor.done();
			
			monitor.beginTask("Logging to TeamCity Server", -1);
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
			final TreeSet<ICredential> ordered = new TreeSet<ICredential>(ourCredentialComaparator);
			ordered.addAll(credentials);
			final ICredential defaultCredential = ordered.iterator().next();
			Logger.log(CommandRunner.class.getSimpleName(), MessageFormat.format("Host \"{0}\" selected as Default for session", defaultCredential.getServer()));
			return defaultCredential.getServer();
		}
		return null;
	}
	
}

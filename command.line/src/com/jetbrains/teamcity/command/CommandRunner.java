package com.jetbrains.teamcity.command;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import jetbrains.buildServer.serverSide.crypt.EncryptUtil;

import org.apache.log4j.Logger;

import com.jetbrains.teamcity.Debug;
import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.resources.ICredential;
import com.jetbrains.teamcity.resources.TCAccess;
import com.jetbrains.teamcity.runtime.ConsoleProgressMonitor;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

public class CommandRunner {
	
//	private static Logger LOGGER = Logger.getLogger(CommandRunner.class) ;
	
	static final String USER_ARG = Messages.getString("CommandRunner.global.runtime.param.user"); //$NON-NLS-1$
	static final String PASSWORD_ARG = Messages.getString("CommandRunner.global.runtime.param.password"); //$NON-NLS-1$
	static final String HOST_ARG = Messages.getString("CommandRunner.global.runtime.param.host"); //$NON-NLS-1$
	
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
		/**
		 * instantiate Debug and set mode according to command line 
		 */
		final Debug debug = Debug.getInstance();
		debug.setDebug(arguments.isDebugOn());
		
		final IProgressMonitor monitor = new ConsoleProgressMonitor(System.out);
		
		final ICommand command = CommandRegistry.getInstance().getCommand(arguments.getCommandId());
		if(command != null){
			try{
				command.validate(arguments);
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
				monitor.done(Messages.getString("CommandRunner.monitor.error.found")); //$NON-NLS-1$
				reportError(command, e, monitor);
				System.exit(-1);
			}
		} else {
			final ICommand helpCommand = CommandRegistry.getInstance().getCommand(Help.ID);
//			newArgs = new String[args.length + 1];
			helpCommand.execute(null, arguments, monitor);
			reportResult(helpCommand, monitor);
		}
	}
	
	private static void reportError(ICommand command, Throwable e, IProgressMonitor monitor) {
		Debug.getInstance().error(CommandRunner.class, e.getMessage(), e);
		if(e instanceof UnknownHostException){
			System.err.println(MessageFormat.format(Messages.getString("CommandRunner.unknown.host.error.pattern"), e.getMessage())); //$NON-NLS-1$
			
		} else if (e instanceof URISyntaxException){
			System.err.println(MessageFormat.format(Messages.getString("CommandRunner.invalid.url.error.pattern"), e.getMessage())); //$NON-NLS-1$
			
		} else if (e instanceof MalformedURLException){
			System.err.println(MessageFormat.format(Messages.getString("CommandRunner.invalid.url.error.pattern"), e.getMessage())); //$NON-NLS-1$
				
		} else if (e instanceof EAuthorizationException){
			System.err.println(MessageFormat.format(Messages.getString("CommandRunner.invalid.credential.error.pattern"), e.getMessage())); //$NON-NLS-1$
			
		} else if (e instanceof ECommunicationException){
			System.err.println(MessageFormat.format(Messages.getString("CommandRunner.could.not.connect.error.pattern"), e.getMessage())); //$NON-NLS-1$
			
		} else if (e instanceof ERemoteError){
			System.err.println(MessageFormat.format(Messages.getString("CommandRunner.businesslogic.error.pattern"), e.getMessage())); //$NON-NLS-1$
			
		} else if (e instanceof IllegalArgumentException){
			System.err.println(MessageFormat.format(Messages.getString("CommandRunner.invalid.command.arguments.error.pattern"), e.getMessage())); //$NON-NLS-1$
			System.err.println();
			System.err.println(command.getUsageDescription());
			
		} else {
			e.printStackTrace();
		}
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
					throw new IllegalArgumentException(MessageFormat.format(Messages.getString("CommandRunner.not.logged.in.error.pattern"), host)); //$NON-NLS-1$
				}
			}
			final Server server = new Server(new URL(host));
			monitor.beginTask(MessageFormat.format(Messages.getString("CommandRunner.connecting.step.name"), host)); //$NON-NLS-1$
			server.connect();
			monitor.done();
			
			monitor.beginTask(Messages.getString("CommandRunner.logging.step.name")); //$NON-NLS-1$
			server.logon(user, password);
			monitor.done();
			return server;
		} else {
			throw new IllegalArgumentException(MessageFormat.format(Messages.getString("CommandRunner.no.default.host.error.pattern"), HOST_ARG)); //$NON-NLS-1$
		}
	}

	private static String getDefaultHost() {
		final Collection<ICredential> credentials = TCAccess.getInstance().credentials();
		if(!credentials.isEmpty()){
			//sort by creation TS. the newest will be used as default 
			final ArrayList<ICredential> ordered = new ArrayList<ICredential>(credentials);
			Collections.sort(ordered, ourCredentialComaparator);
			final ICredential defaultCredential = ordered.iterator().next();
			Debug.getInstance().debug(CommandRunner.class, MessageFormat.format("Using \"{0}\" as Default TeamCity Server", defaultCredential.getServer())); //$NON-NLS-1$
			return defaultCredential.getServer();
		}
		Debug.getInstance().debug(CommandRunner.class, "No Default TeamCity Server found"); //$NON-NLS-1$
		return null;
	}
	
}

package com.jetbrains.teamcity.command;

import java.text.MessageFormat;
import java.util.Collection;

import javax.naming.directory.InvalidAttributesException;

import jetbrains.buildServer.vcs.VcsRoot;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.resources.IShare;
import com.jetbrains.teamcity.resources.TCAccess;

public class Share implements ICommand {
	
	private static final String ID = "share";

	private static final String INFO_PARAM = "-i";
	private static final String INFO_PARAM_LONG = "--info";
	private static final String LOCAL_PARAM = "-l";
	private static final String LOCAL_PARAM_LONG = "--local";
	private static final String VCSROOT_PARAM = "-v";
	private static final String VCSROOT_PARAM_LONG = "--vcsroot";

	public void execute(final Server server, Args args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		
		if (args.hasArgument(VCSROOT_PARAM, VCSROOT_PARAM_LONG) && args.hasArgument(LOCAL_PARAM, LOCAL_PARAM_LONG)) {
			final String localPath = args.getArgument(LOCAL_PARAM, LOCAL_PARAM_LONG);
			final String vcsRootId = args.getArgument(VCSROOT_PARAM, VCSROOT_PARAM_LONG);
			if(vcsRootId != null){
				//check format
				final long id;
				try{
					id = new Long(vcsRootId).longValue();
				} catch (NumberFormatException e){
					throw new IllegalArgumentException(MessageFormat.format("wrong Id format: {0}", vcsRootId), e);
				}
				//try to find root
				for(final VcsRoot root : server.getVcsRoots()){
					if (id == root.getId()) {
						if(localPath == null){
							throw new IllegalArgumentException("no local path passed");
						}
						final String shareId = share(localPath, root).getId();
						System.out.println(MessageFormat.format("{0}", shareId));
						return;
					}
				}
				throw new IllegalArgumentException(MessageFormat.format("no VcsRoot found. id={0}", vcsRootId));
			}
			return;
		} else if (args.hasArgument(INFO_PARAM, INFO_PARAM_LONG)){
			final Collection<IShare> roots = TCAccess.getInstance().roots();
			if(roots.isEmpty()){
				System.out.println("no one share found");
				return;
			}
			System.out.println("id\tlocal\tvcsrootid\tproperties");
			for(final IShare root : roots){
				System.out.println(MessageFormat.format("{0}\t{1}\t{2}\t{3}", root.getId(),  root.getLocal(), String.valueOf(root.getRemote()), root.getProperties()));
			}
			return;
		}
		System.out.println(getUsageDescription());
	}

	private IShare share(final String localPath, final VcsRoot root) {
		return TCAccess.getInstance().share(localPath, root);
	}

	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired(final Args args) {
		return args.hasArgument(VCSROOT_PARAM, VCSROOT_PARAM_LONG) && args.hasArgument(LOCAL_PARAM, LOCAL_PARAM_LONG);
	}

	public String getUsageDescription() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getDescription()).append("\n");
		buffer.append(MessageFormat.format("usage: {0} [{1}[{2}] ARG_VCSROOTID {3}[{4}] ARG_LOCALPATH] | [{5}[{6}]]", 
				getId(), VCSROOT_PARAM, VCSROOT_PARAM_LONG, 
						LOCAL_PARAM, LOCAL_PARAM_LONG,
						INFO_PARAM, INFO_PARAM_LONG)).append("\n");
		buffer.append("\n");
		buffer.append("Create mapping of locat folder to existing TeamCity VcsRoot or show existing shares").append("\n");
		buffer.append("\n");
		buffer.append("Valid options:").append("\n");;
		buffer.append(MessageFormat.format("\t{0}[{1}] ARG_VCSROOTID\t: {2}", VCSROOT_PARAM, VCSROOT_PARAM_LONG, "target TeamCity VcsRoot id. Can be found using by \"info\" command")).append("\n");
		buffer.append(MessageFormat.format("\t{0}[{1}] ARG_LOCALPATH\t: {2}", LOCAL_PARAM, LOCAL_PARAM_LONG, "absolute path to existing local folder will be shared with TeamCity VcsRoot")).append("\n");
		buffer.append(MessageFormat.format("\t{0}[{1}]\t: {2}", INFO_PARAM, INFO_PARAM_LONG, "show existing shares")).append("\n");
		return buffer.toString();
	}
	
	public String getDescription() {
		return "Associate local folder with known TeamCity VcsRoot";
	}
	

}

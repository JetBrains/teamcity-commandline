package com.jetbrains.teamcity.commandline;

import java.text.MessageFormat;
import java.util.Collection;

import javax.naming.directory.InvalidAttributesException;

import jetbrains.buildServer.vcs.VcsRoot;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.resources.IVCSRoot;
import com.jetbrains.teamcity.resources.VCSAccess;

public class Share implements ICommand {

	private static final String ID = "share";

	@Override
	public void execute(final Server server, String[] args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		
		if (Util.hasArgument(args, "-v", "--vcsroot") && Util.hasArgument(args, "-l", "--local")) {
			final String localPath = Util.getArgumentValue(args, "-l", "--local");
			final String vcsRootId = Util.getArgumentValue(args, "-v", "--vcsroot");
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
		} else if (Util.hasArgument(args, "-i", "--info")){
			final Collection<IVCSRoot> roots = VCSAccess.getInstance().roots();
			if(roots.isEmpty()){
				System.out.println("no one share found");
				return;
			}
			System.out.println("id\tlocal\tremote");
			for(final IVCSRoot root : roots){
				final VcsRoot remoteRoot = server.getVcsRoot(root.getRemote());				
				System.out.println(MessageFormat.format("{0}\t{1}\t{2}", root.getId(),  root.getLocal(), String.valueOf(remoteRoot.getId())));
			}
			return;
		}
		System.out.println(getHelp());
	}

	private IVCSRoot share(final String localPath, final VcsRoot root) {
		return VCSAccess.getInstance().share(localPath, root.getId());
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public boolean isConnectionRequired() {
		return true;
	}

	@Override
	public String getHelp() {
		return MessageFormat.format("{0}: use -v [vcs_root_id] -l [local_path]", getId()); 
	}

}

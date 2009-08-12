package com.jetbrains.teamcity.command;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.naming.directory.InvalidAttributesException;

import jetbrains.buildServer.vcs.VcsRoot;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.resources.IShare;
import com.jetbrains.teamcity.resources.TCAccess;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

public class Share implements ICommand {
	
	private static final String ID = Messages.getString("Share.command.id"); //$NON-NLS-1$

	private static final String INFO_PARAM = Messages.getString("Share.info.runtime.param"); //$NON-NLS-1$
	private static final String INFO_PARAM_LONG = Messages.getString("Share.info.runtime.param.long"); //$NON-NLS-1$
	
	private static final String LOCAL_PARAM = Messages.getString("Share.local.runtime.param"); //$NON-NLS-1$
	private static final String LOCAL_PARAM_LONG = Messages.getString("Share.local.runtime.param.long"); //$NON-NLS-1$
	
	private static final String VCSROOT_PARAM = Messages.getString("Share.vcsroot.runtime.param"); //$NON-NLS-1$
	private static final String VCSROOT_PARAM_LONG = Messages.getString("Share.vcsroot.runtime.param.long"); //$NON-NLS-1$

	private String myResultDescription;
	
	public void validate(Args args) throws IllegalArgumentException {
		//no op as far "info" available 
	}

	public void execute(final Server server, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		
		if (args.hasArgument(VCSROOT_PARAM, VCSROOT_PARAM_LONG) && args.hasArgument(LOCAL_PARAM, LOCAL_PARAM_LONG)) {
			final String localPath = args.getArgument(LOCAL_PARAM, LOCAL_PARAM_LONG);
			final String vcsRootId = args.getArgument(VCSROOT_PARAM, VCSROOT_PARAM_LONG);
			if(vcsRootId != null){
				//check format
				final long id;
				try{
					id = new Long(vcsRootId).longValue();
				} catch (NumberFormatException e){
					throw new IllegalArgumentException(MessageFormat.format(Messages.getString("Share.vcsroot.not.integer.error.message"), vcsRootId), e); //$NON-NLS-1$
				}
				//try to find root
				for(final VcsRoot root : server.getVcsRoots()){
					if (id == root.getId()) {
						if(localPath == null){
							throw new IllegalArgumentException(Messages.getString("Share.localpath.not.passed.error.message")); //$NON-NLS-1$
						}
						final String shareId = share(localPath, root).getId();
						myResultDescription = MessageFormat.format(Messages.getString("Share.result.ok.pattern"), shareId, localPath, vcsRootId); //$NON-NLS-1$
						return;
					}
				}
				throw new IllegalArgumentException(MessageFormat.format(Messages.getString("Share.vcsroot.not.found.for.sharing.error.message"), vcsRootId)); //$NON-NLS-1$
			}
			return;
			
		} else if (args.hasArgument(INFO_PARAM, INFO_PARAM_LONG)){ //info branch
			final ArrayList<IShare> roots = new ArrayList<IShare>(TCAccess.getInstance().roots());
			if(roots.isEmpty()){
				myResultDescription = Messages.getString("Share.no.one.share.info.message"); //$NON-NLS-1$
				return;
			}
			Collections.sort(roots, new Comparator<IShare>(){
				public int compare(IShare o1, IShare o2) {
					if (Long.valueOf(o1.getId()) < Long.valueOf(o2.getId())) {
						return -1;
					} else if (Long.valueOf(o1.getId()) > Long.valueOf(o2.getId())) {
						return 1;
					}
					return 0;
				}});
			
			final StringBuffer buffer = new StringBuffer();
			buffer.append(Messages.getString("Share.shares.list.header")); //$NON-NLS-1$
			for(final IShare root : roots){
				buffer.append(MessageFormat.format(Messages.getString("Share.shares.list.pattern"), root.getId(),  root.getLocal(), String.valueOf(root.getRemote()), root.getProperties())); //$NON-NLS-1$
			}
			myResultDescription = buffer.toString();
			return;
		}
		myResultDescription = getUsageDescription();
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
		return MessageFormat.format(Messages.getString("Share.help.usage.pattern"),  //$NON-NLS-1$
				getCommandDescription(), getId(), 
				VCSROOT_PARAM, VCSROOT_PARAM_LONG, 
				LOCAL_PARAM, LOCAL_PARAM_LONG,
				INFO_PARAM, INFO_PARAM_LONG,
				VCSROOT_PARAM, VCSROOT_PARAM_LONG,
				LOCAL_PARAM, LOCAL_PARAM_LONG,INFO_PARAM, INFO_PARAM_LONG);
	}
	
	public String getCommandDescription() {
		return Messages.getString("Share.help.description"); //$NON-NLS-1$
	}
	
	public String getResultDescription() {
		return myResultDescription;
	}
	
	

}

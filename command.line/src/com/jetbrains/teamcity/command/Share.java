package com.jetbrains.teamcity.command;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import jetbrains.buildServer.vcs.VcsRoot;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.URLFactory;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.Util.StringTable;
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
	
	private static final String UPDATE_SWITCH = Messages.getString("Share.sync.runtime.param"); //$NON-NLS-1$
	private static final String UPDATE_SWITCH_LONG = Messages.getString("Share.sync.runtime.param.long"); //$NON-NLS-1$

	private String myResultDescription;
	
	public void validate(Args args) throws IllegalArgumentException {
		//no op as far "info" available 
	}

	public void execute(final Server server, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError {
		
		if (args.hasArgument(VCSROOT_PARAM, VCSROOT_PARAM_LONG)) {
			final String localPath;
			//check exists and use current if omit
			if(args.hasArgument(LOCAL_PARAM, LOCAL_PARAM_LONG)){
				localPath = args.getArgument(LOCAL_PARAM, LOCAL_PARAM_LONG);
			} else {
				localPath = new File(".").getAbsolutePath(); //$NON-NLS-1$
			}
			final String vcsRootId = args.getArgument(VCSROOT_PARAM, VCSROOT_PARAM_LONG);
			if(vcsRootId != null){
				//check format
				final long id;
				try{
					id = new Long(vcsRootId).longValue();
				} catch (NumberFormatException e){
					throw new IllegalArgumentException(MessageFormat.format(Messages.getString("Share.vcsroot.not.integer.error.message"), vcsRootId), e); //$NON-NLS-1$
				}
				myResultDescription = share(server, localPath, id);
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
			
			final StringTable table = new Util.StringTable(Messages.getString("Share.shares.list.header")); //$NON-NLS-1$
			for(final IShare root : roots){
				final String defaultMapping = TCAccess.getInstance().getDefaultMapping(root);
				
				table.addRow(MessageFormat.format(Messages.getString("Share.shares.list.pattern"), //$NON-NLS-1$
						root.getId(),  root.getLocal(), String.valueOf(root.getRemote()), root.getProperties(), 
						defaultMapping != null ? defaultMapping : "")); //$NON-NLS-1$
			}
			myResultDescription = table.toString();
			return;
			
		} else if (args.hasArgument(UPDATE_SWITCH, UPDATE_SWITCH_LONG)){//refresh cached info
			myResultDescription = update(server, args.getLastArgument());
			return;
			
		}
		
		myResultDescription = getUsageDescription();
	}

	private String share(final Server server, String localPath, final long id) throws ECommunicationException {
		//try to find root
		for(final VcsRoot root : server.getVcsRoots()){
			if (id == root.getId()) {
				//create new Share
				final IShare share = TCAccess.getInstance().share(localPath, root);
				//scan for multiple mappings and prompt to select default one
				final URLFactory factory = URLFactory.getFactory(share);
				final String[] mappings = factory.getMappings();
				if (mappings != null && mappings.length > 0) {
					System.out.println(Messages.getString("Share.default.mapping.message")); //$NON-NLS-1$
					final StringTable table = new Util.StringTable(2);
					int i = 0;
					for (; i < mappings.length; i++) {
						table.addRow(MessageFormat.format(Messages.getString("Share.custom.mapping.row.pattern"), String.valueOf(i + 1), mappings[i])); //$NON-NLS-1$
					}
					table.addRow(Messages.getString("Share.custom.mapping.empty.row")); //$NON-NLS-1$
					int doNotSetDefaultMappingIndex = i + 1;
					table.addRow(MessageFormat.format(
							Messages.getString("Share.custom.mapping.do.not.set.default.row.pattern"), String.valueOf(doNotSetDefaultMappingIndex))); //$NON-NLS-1$
					System.out.println(table.toString());
					// wait for number input
					while (true) {
						try{
							final String selection = Util.readConsoleInput(Messages.getString("Share.defauilt.mapping.prompt")); //$NON-NLS-1$
							int choise = Integer.parseInt(selection);
							if (choise > 0 && choise < doNotSetDefaultMappingIndex) {
								setDefault(share, mappings[choise - 1]);
								break;
							} else if (choise == doNotSetDefaultMappingIndex) {
								break;
							}
						} catch (Throwable t){
							//wrong input
						}
					}					
				}
				return MessageFormat.format(Messages.getString("Share.result.ok.pattern"), share.getId(), localPath, String.valueOf(id)); //$NON-NLS-1$
			}
		}
		throw new IllegalArgumentException(MessageFormat.format(Messages.getString("Share.vcsroot.not.found.for.sharing.error.message"), String.valueOf(id))); //$NON-NLS-1$
	}

	private void setDefault(final IShare share, final String mapping) {
		TCAccess.getInstance().setDefaultMapping(share, mapping);
	}

	private String update(final Server server, String shareId) throws ECommunicationException {
		final Collection<IShare> all = TCAccess.getInstance().roots();
		if(all.isEmpty()){
			return Messages.getString("Share.update.nothing.to.update.message"); //$NON-NLS-1$
		}
		int updatedCount = 0;
		int deletedCount = 0;
		for(final IShare share : all){
			if(shareId == null || shareId.equals(share.getId())){
				final Long vcsRootId = share.getRemote();
				final VcsRoot root = server.getVcsRoot(vcsRootId);
				if(root == null){
					TCAccess.getInstance().unshare(share.getId());
					deletedCount ++;
				} else {
					TCAccess.getInstance().update(share, root);
					updatedCount ++;
				}
			}
		}
		return MessageFormat.format(Messages.getString("Share.update.info.pattern"), updatedCount, deletedCount); //$NON-NLS-1$
	}

	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired(final Args args) {
		return (args.hasArgument(VCSROOT_PARAM, VCSROOT_PARAM_LONG)) || args.hasArgument(UPDATE_SWITCH, UPDATE_SWITCH_LONG);
	}

	public String getUsageDescription() {
		return MessageFormat.format(Messages.getString("Share.help.usage.pattern"),  //$NON-NLS-1$
				getCommandDescription(), getId(), 
				VCSROOT_PARAM, VCSROOT_PARAM_LONG, 
				LOCAL_PARAM, LOCAL_PARAM_LONG,
				INFO_PARAM, INFO_PARAM_LONG,
				VCSROOT_PARAM, VCSROOT_PARAM_LONG,
				LOCAL_PARAM, LOCAL_PARAM_LONG,INFO_PARAM, 
				INFO_PARAM_LONG,
				UPDATE_SWITCH, UPDATE_SWITCH_LONG);
	}
	
	public String getCommandDescription() {
		return Messages.getString("Share.help.description"); //$NON-NLS-1$
	}
	
	public String getResultDescription() {
		return myResultDescription;
	}
	
}

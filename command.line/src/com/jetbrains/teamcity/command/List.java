package com.jetbrains.teamcity.command;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.naming.directory.InvalidAttributesException;

import jetbrains.buildServer.BuildTypeData;
import jetbrains.buildServer.ProjectData;
import jetbrains.buildServer.vcs.VcsRoot;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

public class List implements ICommand {

	private static final String EMPTY = Messages.getString("List.empty.description"); //$NON-NLS-1$
	
	private static final String VCSROOT_SWITCH_LONG = Messages.getString("List.vcsroot.runtime.param.long"); //$NON-NLS-1$
	private static final String VCSROOT_SWITCH = Messages.getString("List.vcsroot.runtime.param"); //$NON-NLS-1$
	
	private static final String CONFIGURATION_SWITCH_LONG = Messages.getString("List.conf.runtime.param.long"); //$NON-NLS-1$
	private static final String CONFIGURATION_SWITCH = Messages.getString("List.conf.runtime.param"); //$NON-NLS-1$
	
	private static final String PROJECT_SWITCH_LONG = Messages.getString("List.project.runtime.param.long"); //$NON-NLS-1$
	private static final String PROJECT_SWITCH = Messages.getString("List.project.runtime.param"); //$NON-NLS-1$
	
	private static final String ID = Messages.getString("List.command.id"); //$NON-NLS-1$

	private String myResultDescription;

	public void execute(final Server server, final Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(args.hasArgument(PROJECT_SWITCH, PROJECT_SWITCH_LONG) 
				&& !args.hasArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG)
				&& !args.hasArgument(VCSROOT_SWITCH, VCSROOT_SWITCH_LONG)){
			
			myResultDescription = printProjects(server);
			
		} else if (args.hasArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG)
				&& !args.hasArgument(VCSROOT_SWITCH, VCSROOT_SWITCH_LONG)){
			
			myResultDescription = printConfigurations(server, args);
			
		} else if (args.hasArgument(VCSROOT_SWITCH, VCSROOT_SWITCH_LONG) 
				&& !args.hasArgument(PROJECT_SWITCH, PROJECT_SWITCH_LONG)){
			
			myResultDescription = printVcsRoots(server, args);
			
		} else {
			
			final StringBuffer resultBuffer = new StringBuffer();
			resultBuffer.append(Messages.getString("List.projects.section.header")); //$NON-NLS-1$
			resultBuffer.append(printProjects(server));
			resultBuffer.append(Messages.getString("List.configurations.section.header")); //$NON-NLS-1$
			resultBuffer.append(printConfigurations(server, null));
			resultBuffer.append(Messages.getString("List.vcsroots.section.header")); //$NON-NLS-1$
			resultBuffer.append(printVcsRoots(server, null));
			
			myResultDescription = resultBuffer.toString();
		}
	}

	private String printVcsRoots(final Server server, Args args)	throws ECommunicationException {
		final StringBuffer buffer = new StringBuffer();
		final ArrayList<? extends VcsRoot> roots;
		if(args != null && args.hasArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG)){
			final String filterByConfig = args.getArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG);
			final BuildTypeData config = server.getConfiguration(filterByConfig);
			if(config == null){
				buffer.append(MessageFormat.format(Messages.getString("List.no.config.found.for.filter.message"), filterByConfig)); //$NON-NLS-1$
				return buffer.toString();
			}
			roots = new ArrayList<VcsRoot>(config.getVcsRoots());
		} else {
			roots = new ArrayList<VcsRoot>(server.getVcsRoots());
		}
		Collections.sort(roots, new Comparator<VcsRoot>(){
			public int compare(VcsRoot o1, VcsRoot o2) {
				if (o1.getId() < o2.getId()) {
					return -1;
				} else if (o1.getId() > o2.getId()) {
					return 1;
				}
				return 0;
			}});
		buffer.append(Messages.getString("List.vcsroots.list.header")); //$NON-NLS-1$
		for(final VcsRoot root :roots){
			String name = root.getName()== null ? EMPTY : root.getName();
			buffer.append(MessageFormat.format(Messages.getString("List.vcsroots.list.pattern"), root.getId(), name, root.getVcsName(), root.getProperties()));	 //$NON-NLS-1$
		}
		return buffer.toString();
	}

	private String printConfigurations(final Server server, Args args) throws ECommunicationException {
		final StringBuffer buffer = new StringBuffer();
		String filterByProject = null;
		if(args != null && args.hasArgument(PROJECT_SWITCH, PROJECT_SWITCH_LONG)){
			filterByProject = args.getArgument(PROJECT_SWITCH, PROJECT_SWITCH_LONG);
		}
		//get & sort
		final ArrayList<BuildTypeData> configurations = new ArrayList<BuildTypeData>(server.getConfigurations());
		Collections.sort(configurations, new Comparator<BuildTypeData>(){
			public int compare(BuildTypeData o1, BuildTypeData o2) {
				return o1.getId().compareTo(o2.getId());
			}});
		//display
		buffer.append(Messages.getString("List.config.list.header")); //$NON-NLS-1$
		for(final BuildTypeData config :configurations){
			//check 
			if((filterByProject == null) || (filterByProject != null && config.getProjectId().equals(filterByProject))){
				String description = config.getDescription() == null ? EMPTY : config.getDescription();
				buffer.append(MessageFormat.format(Messages.getString("List.config.list.pattern"), config.getId(), config.getName(), config.getStatus(), description));	 //$NON-NLS-1$
			}
		}
		return buffer.toString();
	}

	private String printProjects(final Server server) throws ECommunicationException {
		final StringBuffer buffer = new StringBuffer(); 
		//get & sort
		final ArrayList<ProjectData> projects = new ArrayList<ProjectData>(server.getProjects());
		Collections.sort(projects, new Comparator<ProjectData>(){
			public int compare(ProjectData o1, ProjectData o2) {
				return o1.getProjectId().compareTo(o2.getProjectId());
			}});
		
		//display
		buffer.append(Messages.getString("List.project.list.header")); //$NON-NLS-1$
		for(final ProjectData project :projects){
			buffer.append(MessageFormat.format(Messages.getString("List.project.list.pattern"), project.getProjectId(), project.getName(), project.getStatus(), project.getDescription())); //$NON-NLS-1$
		}
		return buffer.toString();
	}

	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired(final Args args) {
		return true;
	}

	public String getUsageDescription() {
		return MessageFormat.format(Messages.getString("List.help.usage.pattern"),  //$NON-NLS-1$
				getCommandDescription(), getId(), PROJECT_SWITCH , CONFIGURATION_SWITCH, VCSROOT_SWITCH, PROJECT_SWITCH, PROJECT_SWITCH_LONG, CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG, VCSROOT_SWITCH, VCSROOT_SWITCH_LONG);
	}

	public String getCommandDescription() {
		return Messages.getString("List.help.description"); //$NON-NLS-1$
	}
	
	public String getResultDescription() {
		return myResultDescription;
	}
	
	

}

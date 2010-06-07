package com.jetbrains.teamcity.command;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.naming.directory.InvalidAttributesException;

import jetbrains.buildServer.BuildTypeData;
import jetbrains.buildServer.ProjectData;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.Util.StringTable;
import com.jetbrains.teamcity.runtime.IProgressMonitor;

public class List implements ICommand {

	private static final String EMPTY = Messages.getString("List.empty.description"); //$NON-NLS-1$
	
	private static final String CONFIGURATION_SWITCH_LONG = Messages.getString("List.conf.runtime.param.long"); //$NON-NLS-1$
	private static final String CONFIGURATION_SWITCH = Messages.getString("List.conf.runtime.param"); //$NON-NLS-1$
	
	private static final String PROJECT_SWITCH_LONG = Messages.getString("List.project.runtime.param.long"); //$NON-NLS-1$
	private static final String PROJECT_SWITCH = Messages.getString("List.project.runtime.param"); //$NON-NLS-1$
	
	private static final String ID = Messages.getString("List.command.id"); //$NON-NLS-1$

	private String myResultDescription;

	public void execute(final Server server, final Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(args.hasArgument(PROJECT_SWITCH, PROJECT_SWITCH_LONG) 
				&& !args.hasArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG)){
			
			myResultDescription = printProjects(server);
			
		} else if (args.hasArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG)){
			
			myResultDescription = printConfigurations(server, args);
			
		}  else {
			
			final StringBuffer resultBuffer = new StringBuffer();
			resultBuffer.append(Messages.getString("List.projects.section.header")); //$NON-NLS-1$
			resultBuffer.append(printProjects(server));
			resultBuffer.append(Messages.getString("List.configurations.section.header")); //$NON-NLS-1$
			resultBuffer.append(printConfigurations(server, null));
			
			myResultDescription = resultBuffer.toString();
		}
	}

	private String printConfigurations(final Server server, Args args) throws ECommunicationException {
		final String filterByProject;
		if(args != null && args.hasArgument(PROJECT_SWITCH, PROJECT_SWITCH_LONG)){
			filterByProject = args.getArgument(PROJECT_SWITCH, PROJECT_SWITCH_LONG);
		} else {
			filterByProject = null;
		}
		//get & sort
		//cache projects
		final HashMap<String, String> prjMap = new HashMap<String, String>();
		for(final ProjectData prj :server.getProjects()){
			prjMap.put(prj.getProjectId(), prj.getName());
		}
		final ArrayList<BuildTypeData> configurations = new ArrayList<BuildTypeData>(server.getConfigurations());
		Collections.sort(configurations, new Comparator<BuildTypeData>(){
			public int compare(BuildTypeData o1, BuildTypeData o2) {
				if(filterByProject !=  null){
					return o1.getName().compareTo(o2.getName());	
				}
				final String prj1name = prjMap.get(o1.getProjectId());
				final String prj2name = prjMap.get(o2.getProjectId());
				return (prj1name + " " + o1.getName()).compareTo(prj2name + " " + o2.getName());
			}});
		//display
		final StringTable table = new Util.StringTable(Messages.getString("List.config.list.header")); //$NON-NLS-1$
		for(final BuildTypeData config :configurations){
			//check 
			if((filterByProject == null) || (filterByProject != null && config.getProjectId().equals(filterByProject))){
				final String description = config.getDescription() == null ? EMPTY : config.getDescription();
				final String prjName = prjMap.get(config.getProjectId());
				table.addRow(MessageFormat.format(Messages.getString("List.config.list.pattern"), config.getId(), prjName, config.getName(), config.getStatus(), description));	 //$NON-NLS-1$
			}
		}
		return table.toString();
	}

	private String printProjects(final Server server) throws ECommunicationException {
		//get & sort
		final ArrayList<ProjectData> projects = new ArrayList<ProjectData>(server.getProjects());
		Collections.sort(projects, new Comparator<ProjectData>(){
			public int compare(ProjectData o1, ProjectData o2) {
				return o1.getName().compareTo(o2.getName());
			}});
		
		//display
		final StringTable table = new Util.StringTable(Messages.getString("List.project.list.header")); //$NON-NLS-1$
		for(final ProjectData project :projects){
			final String description = project.getDescription() == null ? EMPTY : project.getDescription();
			table.addRow(MessageFormat.format(Messages.getString("List.project.list.pattern"), project.getProjectId(), project.getName(), project.getStatus(), description)); //$NON-NLS-1$
		}
		return table.toString();
	}

	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired(final Args args) {
		return true;
	}

	public String getUsageDescription() {
		return MessageFormat.format(Messages.getString("List.help.usage.pattern"),  //$NON-NLS-1$
				getCommandDescription(), getId(), 
				PROJECT_SWITCH , 
				CONFIGURATION_SWITCH, 
				PROJECT_SWITCH,
				PROJECT_SWITCH,
				PROJECT_SWITCH_LONG, 
				CONFIGURATION_SWITCH, 
				CONFIGURATION_SWITCH_LONG);
	}

	public String getCommandDescription() {
		return Messages.getString("List.help.description"); //$NON-NLS-1$
	}
	
	public String getResultDescription() {
		return myResultDescription;
	}

	public void validate(Args args) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}
	
	

}

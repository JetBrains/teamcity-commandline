package com.jetbrains.teamcity.command;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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
		//cache projects
		final HashMap<String, ProjectData> prjMap = new HashMap<String, ProjectData>();
		for(final ProjectData prj :server.getProjects()){
			prjMap.put(prj.getProjectId(), prj);
		}
		//cache config
		final HashMap<String, BuildTypeData> configMap = new HashMap<String, BuildTypeData>();
		for(final BuildTypeData config:server.getConfigurations()){
			configMap.put(config.getId(), config);
		}
		//sort prjname+configname+vcsrootname
		Collections.sort(roots, new Comparator<VcsRoot>(){
			public int compare(VcsRoot o1, VcsRoot o2) {
				//config
				final Collection<BuildTypeData> configurations1 = collectConfigurations(configMap.values(), o1);
				final String c1names = asString(configurations1);
				final Collection<BuildTypeData> configurations2 = collectConfigurations(configMap.values(), o2);
				final String c2names = asString(configurations2);
				//prj
				final String prj1names = List.this.toString(collectProjects(prjMap, configurations1));
				final String prj2names = List.this.toString(collectProjects(prjMap, configurations2));
				//self
				return (prj1names + " " + c1names + " " + (o1.getName()== null ? EMPTY : o1.getName()))
					.compareTo(prj2names + " " + c2names + " " + (o2.getName()== null ? EMPTY : o2.getName()));
//				if (o1.getId() < o2.getId()) {
//					return -1;
//				} else if (o1.getId() > o2.getId()) {
//					return 1;
//				}
//				return 0;
			}});
		buffer.append(Messages.getString("List.vcsroots.list.header")); //$NON-NLS-1$
		for(final VcsRoot root :roots){
			String name = root.getName()== null ? EMPTY : root.getName();
			final Collection<BuildTypeData> configurations = collectConfigurations(configMap.values(), root);
			final String configNames = asString(configurations);
			final String prjNames = toString(collectProjects(prjMap, configurations));
			buffer.append(MessageFormat.format(Messages.getString("List.vcsroots.list.pattern"), root.getId(), prjNames, configNames, name, root.getVcsName(), root.getProperties()));	 //$NON-NLS-1$
		}
		return buffer.toString();
	}
	
	private String asString(Collection<BuildTypeData> configurations){
		String devider ="";
		final StringBuffer buffer = new StringBuffer();
		for(final BuildTypeData config : configurations){
			buffer.append(devider).append(config.getName());
			devider = ",";
		}
		return buffer.toString();
	}

	/**
	 * collect configurations which has passed root 
	 */
	private Collection<BuildTypeData> collectConfigurations(final Collection<BuildTypeData> ccache, final VcsRoot root){
		final ArrayList<BuildTypeData> buffer = new ArrayList<BuildTypeData>();
		config: for(BuildTypeData config : ccache){
			for(final VcsRoot tested : config.getVcsRoots()){
				if(tested.getId()==root.getId()){
					buffer.add(config);
					continue config;
				}
			}
		}
		//sort by name
		Collections.sort(buffer, new Comparator<BuildTypeData>(){
			public int compare(BuildTypeData o1, BuildTypeData o2) {
				return o1.getName().compareTo(o2.getName());
			}});
		return buffer;
	}
	
	/**
	 * collect configurations which has passed root 
	 */
	private Collection<ProjectData> collectProjects(final Map<String, ProjectData> pcache,  final Collection<BuildTypeData> configurations){
		final ArrayList<ProjectData> buffer = new ArrayList<ProjectData>();
		for(BuildTypeData config : configurations){
			final ProjectData prj = pcache.get(config.getProjectId());
			if(prj != null){
				buffer.add(prj);	
			}
		}
		//sort by name
		Collections.sort(buffer, new Comparator<ProjectData>(){
			public int compare(ProjectData o1, ProjectData o2) {
				return o1.getName().compareTo(o2.getName());
			}});
		return buffer;
	}

	private String toString(Collection<ProjectData> configurations){
		String devider ="";
		final StringBuffer buffer = new StringBuffer();
		for(final ProjectData config : configurations){
			buffer.append(devider).append(config.getName());
			devider = ",";
		}
		return buffer.toString();
	}



	private String printConfigurations(final Server server, Args args) throws ECommunicationException {
		final StringBuffer buffer = new StringBuffer();
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
		buffer.append(Messages.getString("List.config.list.header")); //$NON-NLS-1$
		for(final BuildTypeData config :configurations){
			//check 
			if((filterByProject == null) || (filterByProject != null && config.getProjectId().equals(filterByProject))){
				final String description = config.getDescription() == null ? EMPTY : config.getDescription();
				final String prjName = prjMap.get(config.getProjectId());
				buffer.append(MessageFormat.format(Messages.getString("List.config.list.pattern"), config.getId(), prjName, config.getName(), config.getStatus(), description));	 //$NON-NLS-1$
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
				return o1.getName().compareTo(o2.getName());
			}});
		
		//display
		buffer.append(Messages.getString("List.project.list.header")); //$NON-NLS-1$
		for(final ProjectData project :projects){
			final String description = project.getDescription();// == null ? EMPTY : project.getDescription();
			buffer.append(MessageFormat.format(Messages.getString("List.project.list.pattern"), project.getProjectId(), project.getName(), project.getStatus(), description)); //$NON-NLS-1$
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

	public void validate(Args args) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}
	
	

}

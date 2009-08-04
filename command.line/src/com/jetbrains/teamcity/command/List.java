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

	private static final String EMPTY = "<empty>";
	private static final String VCSROOT_SWITCH_LONG = "--vcsroots";
	private static final String VCSROOT_SWITCH = "-v";
	private static final String CONFIGURATION_SWITCH_LONG = "--configurations";
	private static final String CONFIGURATION_SWITCH = "-c";
	private static final String PROJECT_SWITCH_LONG = "--projects";
	private static final String PROJECT_SWITCH = "-p";
	private static final String ID = "info";

	public void execute(final Server server, final Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(args.hasArgument(PROJECT_SWITCH, PROJECT_SWITCH_LONG) 
				&& !args.hasArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG)
				&& !args.hasArgument(VCSROOT_SWITCH, VCSROOT_SWITCH_LONG)){
			printProjects(server);
			
		} else if (args.hasArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG)
				&& !args.hasArgument(VCSROOT_SWITCH, VCSROOT_SWITCH_LONG)){
			printConfigurations(server, args);
			
		} else if (args.hasArgument(VCSROOT_SWITCH, VCSROOT_SWITCH_LONG) 
				&& !args.hasArgument(PROJECT_SWITCH, PROJECT_SWITCH_LONG)){
			printVcsRoots(server, args);
			
		} else {
			System.out.println("projects:");
			printProjects(server);
			System.out.println("configurations:");
			printConfigurations(server, null);
			System.out.println("vcsroots:");
			printVcsRoots(server, null);
		}
	}

	private void printVcsRoots(final Server server, Args args)	throws ECommunicationException {
		final ArrayList<? extends VcsRoot> roots;
		if(args != null && args.hasArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG)){
			final String filterByConfig = args.getArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG);
			final BuildTypeData config = server.getConfiguration(filterByConfig);
			if(config == null){
				System.out.println(MessageFormat.format("No \"{0}\" Configuration found", filterByConfig));
				return;
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
		System.out.println(MessageFormat.format("id\tname\tvcsname\tproperties", ""));
		for(final VcsRoot root :roots){
			String name = root.getName()== null ? EMPTY : root.getName();
			System.out.println(MessageFormat.format("{0}\t{1}\t{2}\t{3}", root.getId(), name, root.getVcsName(), root.getProperties()));	
		}
	}

	private void printConfigurations(final Server server, Args args) throws ECommunicationException {
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
		System.out.println(MessageFormat.format("id\tname\tstatus\tdescription", ""));
		for(final BuildTypeData config :configurations){
			//check 
			if((filterByProject == null) || (filterByProject != null && config.getProjectId().equals(filterByProject))){
				String description = config.getDescription() == null ? EMPTY : config.getDescription();
				System.out.println(MessageFormat.format("{0}\t{1}\t{2}\t{3}", config.getId(), config.getName(), config.getStatus(), description));	
			}
		}
	}

	private void printProjects(final Server server) throws ECommunicationException {
		//get & sort
		final ArrayList<ProjectData> projects = new ArrayList<ProjectData>(server.getProjects());
		Collections.sort(projects, new Comparator<ProjectData>(){
			public int compare(ProjectData o1, ProjectData o2) {
				return o1.getProjectId().compareTo(o2.getProjectId());
			}});
		
		//display
		System.out.println(MessageFormat.format("id\tname\tstatus\tdescription", ""));
		for(final ProjectData project :projects){
			System.out.println(MessageFormat.format("{0}\t{1}\t{2}\t{3}", project.getProjectId(), project.getName(), project.getStatus(), project.getDescription()));
		}
	}

	public String getId() {
		return ID;
	}

	public boolean isConnectionRequired(final Args args) {
		return true;
	}

	public String getUsageDescription() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getDescription()).append("\n");
		buffer.append(MessageFormat.format("usage: {0} [{1}|{2}|{3}]", getId(), PROJECT_SWITCH , CONFIGURATION_SWITCH, VCSROOT_SWITCH)).append("\n");
		buffer.append("\n");
		buffer.append("With no args, print all information of the target TeamCity Server").append("\n");;
		buffer.append("\n");
		buffer.append("Valid options:").append("\n");;
		buffer.append(MessageFormat.format("\t{0}[{1}] \t: {2}", PROJECT_SWITCH, PROJECT_SWITCH_LONG, "display Projects")).append("\n");;
		buffer.append(MessageFormat.format("\t{0}[{1}] \t: {2}", CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG, "display Configurations")).append("\n");;
		buffer.append(MessageFormat.format("\t{0}[{1}] \t: {2}", VCSROOT_SWITCH, VCSROOT_SWITCH_LONG, "display VcsRoots")).append("\n");;
		return buffer.toString();
	}

	public String getDescription() {
		return "Show information for known TeamCity projects, configurations or vcsroots";
	}

}

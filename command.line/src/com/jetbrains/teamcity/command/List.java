package com.jetbrains.teamcity.command;

import java.text.MessageFormat;
import java.util.Collection;

import javax.naming.directory.InvalidAttributesException;

import jetbrains.buildServer.BuildTypeData;
import jetbrains.buildServer.ProjectData;
import jetbrains.buildServer.vcs.VcsRoot;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;

public class List implements ICommand {

	private static final String VCSROOT_SWITCH_LONG = "--vcsroots";
	private static final String VCSROOT_SWITCH = "-v";
	private static final String CONFIGURATION_SWITCH_LONG = "--configurations";
	private static final String CONFIGURATION_SWITCH = "-c";
	private static final String PROJECT_SWITCH_LONG = "--projects";
	private static final String PROJECT_SWITCH = "-p";
	private static final String ID = "info";

	public void execute(final Server server, final Args args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(args.hasArgument(PROJECT_SWITCH, PROJECT_SWITCH_LONG)){
			printProjects(server);
			
		} else if (args.hasArgument(CONFIGURATION_SWITCH, CONFIGURATION_SWITCH_LONG)){
			printConfigurations(server);
			
		}else if (args.hasArgument(VCSROOT_SWITCH, VCSROOT_SWITCH_LONG)){
			printVcsRoots(server);
			
		} else {
			System.out.println(getUsageDescription());
			System.out.println();
			System.out.println("projects:");
			printProjects(server);
			System.out.println("configurations:");
			printConfigurations(server);
			System.out.println("vcsroots:");
			printVcsRoots(server);
		}
	}

	private void printVcsRoots(final Server server)
			throws ECommunicationException {
		final Collection<VcsRoot> roots = server.getVcsRoots();
		System.out.println(MessageFormat.format("id\tname\tvcsname\tproperties", ""));
		for(final VcsRoot root :roots){
			System.out.println(MessageFormat.format("{0}\t{1}\t{2}\t{3}", root.getId(), root.getName(), root.getVcsName(), root.getProperties()));
		}
	}

	private void printConfigurations(final Server server)
			throws ECommunicationException {
		final Collection<BuildTypeData> configurations = server.getConfigurations();
		System.out.println(MessageFormat.format("id\tname\tstatus\tdescription", ""));
		for(final BuildTypeData config :configurations){
			System.out.println(MessageFormat.format("{0}\t{1}\t{2}\t{3}", config.getId(), config.getName(), config.getStatus(), config.getDescription()));
		}
	}

	private void printProjects(final Server server) throws ECommunicationException {
		final Collection<ProjectData> projects = server.getProjects();
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

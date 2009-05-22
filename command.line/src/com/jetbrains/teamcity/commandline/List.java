package com.jetbrains.teamcity.commandline;

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
import com.jetbrains.teamcity.Util;

public class List implements ICommand {

	private static final String ID = "info";

	@Override
	public void execute(final Server server, String[] args) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
		if(Util.hasArgument(args, "-p", "--projects")){
			printProjects(server);
			
		} else if (Util.hasArgument(args, "-c", "--configurations")){
			printConfigurations(server);
			
		}else if (Util.hasArgument(args, "-v", "--vcsroots")){
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

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public boolean isConnectionRequired() {
		return true;
	}

	@Override
	public String getUsageDescription() {
		return MessageFormat.format("{0}: use -p[rojects],-c[onfigurations],-v[csroots] switches", getId()); 
	}

	@Override
	public String getDescription() {
		return "Shows information for known TeamCity projects, configurations or vcsroots";
	}

}

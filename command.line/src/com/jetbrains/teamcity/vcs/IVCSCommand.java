package com.jetbrains.teamcity.vcs;

import java.util.Collection;

public interface IVCSCommand {
	
	public Collection<String> execute(final String root, final CommandLineArguments arguments) throws ETeamException;

}
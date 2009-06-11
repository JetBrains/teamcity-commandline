package com.jetbrains.teamcity.vcs;

public interface IVCSCommandFactory {
	
	public IVCSCommand getModifiedCommand();

	public IVCSCommand getNewCommand();

	public IVCSCommand getDeletedCommand();

	public IVCSCommand getConflictCommand();

	public IVCSCommand getContentCommand();
	
	public IVCSCommand getAddCommand();
	
	public IVCSCommand getCommiCommand();

}
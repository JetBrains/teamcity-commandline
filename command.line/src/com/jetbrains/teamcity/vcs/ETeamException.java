package com.jetbrains.teamcity.vcs;


public class ETeamException extends Exception {

	public ETeamException(Throwable e) {
		super(e);
	}
	
	public ETeamException(String message, Throwable e) {
		super(message, e);
	}

	private static final long serialVersionUID = 1L;

}

package com.jetbrains.teamcity;

public class ERemoteError extends Exception {

	public ERemoteError(String failureReason) {
		super(failureReason);
	}

	private static final long serialVersionUID = 1L;

}

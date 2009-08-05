package com.jetbrains.teamcity.runtime;

public class NullProgressMonitor implements IProgressMonitor {

	public IProgressMonitor beginTask(String name) {
		return this;
	}

	public IProgressMonitor done(String message) {
		return this;
	}

	public IProgressMonitor done() {
		return this;
	}

	public IProgressMonitor worked(String step) {
		return this;
	}

}

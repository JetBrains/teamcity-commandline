package com.jetbrains.teamcity.vcs;

import java.util.Collection;
import java.util.LinkedList;

public class CommandLineArguments extends LinkedList<String>{

	public void setComment(String comments) {
		myComments = comments;
	}

	public String getComment() {
		return myComments;
	}

	private static final long serialVersionUID = 1L;
	private String myComments;
	
	public CommandLineArguments(){
		
	}
	
	public CommandLineArguments(String comments){
		myComments = comments;
	}

	public CommandLineArguments(final Collection<String> strings) {
		addAll(strings);
	}
	
	

}

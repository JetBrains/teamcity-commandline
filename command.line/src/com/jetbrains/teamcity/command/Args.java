package com.jetbrains.teamcity.command;

import com.jetbrains.teamcity.Util;

public class Args {
	
	private String[] myArgs;
	private String myCommandId;

	public Args(final String[] args) {
		if (args == null || args.length == 0) {
			myArgs = new String[] { Help.ID };
			return;
		}
		//extract command from line
		myCommandId = args[0];
		myArgs = new String[args.length -1];
		System.arraycopy(args, 1, myArgs, 0, myArgs.length);
	}
	
	public String getCommandId(){
		return myCommandId;
	}
	
	public boolean hasArgument(final String ...arguments){
		return Util.hasArgument(myArgs, arguments);
	}
	
	public String getArgument(final String ...arguments){
		return Util.getArgumentValue(myArgs, arguments);
	}
	
	public String[] getArguments(){
		return myArgs;
	}

	public String getLastArgument() {
		if (myArgs != null && myArgs.length > 0) {
			return myArgs[myArgs.length - 1];
		}
		return null;
	}
	
}

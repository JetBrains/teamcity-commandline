package com.jetbrains.teamcity.command;

import java.util.Arrays;
import java.util.LinkedList;

import com.jetbrains.teamcity.Util;

public class Args {
	
	public static final String DEBUG_ARG = Messages.getString("Args.debug.switch.name"); //$NON-NLS-1$
	
	public static final String DEBUG_CLEAN_OFF = Messages.getString("Args.do.not.delete.file.after.run.switch.name"); //$NON-NLS-1$
	
	private String[] myArgs;
	private String myCommandId;

	private boolean isDebugOn;

	private boolean isCleanOff;

	public Args(final String[] args) {
		if (args == null || args.length == 0) {
			myArgs = new String[] { Help.ID };
			return;
		}
		final LinkedList<String> list = new LinkedList<String>(Arrays.asList(args));
		//extract CommandId
		myCommandId = list.get(0);
		list.remove(0);
		//remove -debug argument
		if(list.contains(DEBUG_ARG)){
			list.remove(DEBUG_ARG);
			isDebugOn = true;
		}
		//remove -debug-clean-off argument
		if(list.contains(DEBUG_CLEAN_OFF)){
			list.remove(DEBUG_CLEAN_OFF);
			isCleanOff = true;
		}
		myArgs = list.toArray(new String[list.size()]);
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
	
	@Override
	public String toString(){
		return String.format("", myCommandId, Arrays.toString(myArgs)); //$NON-NLS-1$
	}
	
	public boolean isDebugOn (){
		return isDebugOn;
	}
	
	public boolean isCleanOff() {
		return isCleanOff;
	}
	
}

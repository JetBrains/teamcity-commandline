package com.jetbrains.teamcity.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;

import com.jetbrains.teamcity.Util;

public class Args {
	
	public static final String DEBUG_ARG = Messages.getString("Args.debug.switch.name"); //$NON-NLS-1$
	
	public static final String DEBUG_CLEAN_OFF = Messages.getString("Args.do.not.delete.file.after.run.switch.name"); //$NON-NLS-1$
	
	private static HashMap<String, Pattern> ourRegisteredArgs = new HashMap<String, Pattern>();
	
	private String[] myArgs;
	
	private String myArgsLine = "";
	
	private String myCommandId;

	private boolean isDebugOn;

	private boolean isCleanOff;
	
	public static void registerArgument(final String argName, final String argPattern){
		final Pattern pattern = Pattern.compile(argPattern);
		ourRegisteredArgs.put(argName, pattern);
	}
	
	public Args(final String argline) {
		this(argline.split("\\s+"));
	}

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
		for(String arg : myArgs){
			myArgsLine += arg + " ";
		}
		myArgsLine = myArgsLine.trim();
	}
	
	public String getCommandId(){
		return myCommandId;
	}
	
	public boolean hasArgument(final String ...arguments){
		boolean match = false;
		boolean registeredFound = false; 
		for(final String arg : arguments){
			if(ourRegisteredArgs.containsKey(arg)){
				registeredFound = true;
				final Pattern pattern = ourRegisteredArgs.get(arg);
				match |= pattern.matcher(myArgsLine).matches();
			}
		}
		if(registeredFound){
			return match;
		}
		
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

package com.jetbrains.teamcity;



public class Logger {
	
	private static com.intellij.openapi.diagnostic.Logger ourLogger = com.intellij.openapi.diagnostic.Logger.getInstance(Logger.class.getName());
	
	public static void log(Object source, Throwable e) {
		ourLogger.error(e);
	}
	
	public static void log(Object source, String message) {
		ourLogger.debug(message);
	}

}

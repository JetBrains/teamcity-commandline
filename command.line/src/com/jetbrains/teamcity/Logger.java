package com.jetbrains.teamcity;

import java.util.logging.Level;


public class Logger {
	
	private static java.util.logging.Logger ourLogger = java.util.logging.Logger.getLogger(Logger.class.getName());
	
	public static void log(Object source, Throwable e) {
		ourLogger.log(Level.SEVERE, e.getMessage(), e);
	}
	
	public static void log(Object source, String message) {
		ourLogger.log(Level.INFO, message);
	}

}

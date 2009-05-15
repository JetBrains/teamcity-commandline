package com.jetbrains.teamcity;

import java.text.MessageFormat;

public class Logger {
	
	public static void log(Object source, Throwable e) {
		//TODO: implement it
		e.printStackTrace();
	}
	
	public static void log(Object source, String message) {
		//TODO: implement it
		System.out.println(MessageFormat.format("{0}: {1}", String.valueOf(source), message));
	}

}

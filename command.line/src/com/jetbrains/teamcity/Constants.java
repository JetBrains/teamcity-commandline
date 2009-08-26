package com.jetbrains.teamcity;

public class Constants {
	
	public static final int DEFAULT_XMLRPC_TIMEOUT = 1000 * 60 * 10;//10 min
	
	public static final String XMLRPC_TIMEOUT_SYSTEM_PROPERTY = "teamcity.xmlrpc.timeout";
	
	public static final String PERFORCE_P4PORT = "teamcity.vcs.p4port";
	public static final String PERFORCE_P4CLIENT = "teamcity.vcs.p4client";
	public static final String PERFORCE_P4USER = "teamcity.vcs.p4user";
	public static final String PERFORCE_P4PASSWORD = "teamcity.vcs.p4password";

}

package com.jetbrains.teamcity.resources;

import java.util.Map;


public interface IShare {
	
	public String getId();

	public Long getRemote();
	
	public String getLocal();
	
	public String getVcs();
	
	public Map<String, String> getProperties();
}

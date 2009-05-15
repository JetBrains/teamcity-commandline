package com.jetbrains.teamcity.resources;


public interface IVCSRoot {
	
	public String getId();

	public Long getRemote();
	
	public String getLocal();
}

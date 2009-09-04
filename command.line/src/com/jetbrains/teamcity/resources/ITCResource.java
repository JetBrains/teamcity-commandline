package com.jetbrains.teamcity.resources;

import java.io.File;

public interface ITCResource {
	
	public File getLocal();
	
	public String getRepositoryPath();
}

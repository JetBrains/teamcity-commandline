package com.jetbrains.teamcity;

import org.junit.Test;

public class VersionTest {
	
	@Test
	public void showVersion() {
		System.out.println(String.format("Testing %s build...", Build.build));
	}
	
}

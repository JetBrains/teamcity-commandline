package com.jetbrains.teamcity;

import org.junit.Test;

public class _VersionTest {
	
	@Test
	public void showVersion() {
		System.out.println(String.format("Testing %s build...", Build.build));
	}
	
}

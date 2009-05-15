package com.jetbrains.teamcity.commandline;

import org.junit.Test;

public class CommandRegistryTest {
	
	@Test
	public void registerDynamically() throws Exception {
		CommandRegistry.getInstance().registerCommands();
	}

}

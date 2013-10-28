package com.jetbrains.teamcity.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CommandRegistryTest {
	
	@Test
	public void registerDynamically() throws Exception {
//		CommandRegistry.getInstance().registerCommands();
//		final ICommand testCommand = CommandRegistry.getInstance().getCommand(new TestCommand().getId());
//		assertNotNull(testCommand);
//		assertEquals(TestCommand.class, testCommand.getClass());
	}
	
	@Test
	public void isIgnoredJar() throws Exception {
		assertTrue(CommandRegistry.getInstance().isCommandExtensionJar("rt.jar"));
		assertFalse(CommandRegistry.getInstance().isCommandExtensionJar("test.tc.command.12345.jar"));
	}
	
	
	

}

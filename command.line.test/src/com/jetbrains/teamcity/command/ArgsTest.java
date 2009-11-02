package com.jetbrains.teamcity.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;


public class ArgsTest {

	@BeforeClass
	public static void setup() throws Exception {
		new CommandRunner();//register patterns  
	}	

	@Test
	public void HOST_ARG() throws Exception {
		{
			final String command = "run  -m  --host ";
			final Args args = new Args(command);
			boolean host = args.hasArgument(CommandRunner.HOST_ARG);
			assertFalse(host);
		}
		{
			String host = "http://127.0.0.1:8111";
			final String command = String.format("run  -m  %s    %s   ", CommandRunner.HOST_ARG, host);
			final Args args = new Args(command);
			assertTrue(args.hasArgument(CommandRunner.HOST_ARG));
			assertEquals(host, args.getArgument(CommandRunner.HOST_ARG));
		}
		{
			String host = "https://127.0.0.1:8111";
			final String command = String.format("run  -m  %s    %s   ", CommandRunner.HOST_ARG, host);
			final Args args = new Args(command);
			assertTrue(args.hasArgument(CommandRunner.HOST_ARG));
			assertEquals(host, args.getArgument(CommandRunner.HOST_ARG));
		}

	}
	
	
	@Test
	public void USER_ARG() throws Exception {
		{
			final String command = String.format("run   %s  ", CommandRunner.USER_ARG);
			final Args args = new Args(command);
			boolean host = args.hasArgument(CommandRunner.USER_ARG);
			assertFalse(host);
		}
		{
			final String user = "xzxzxzx";
			final String command = String.format("run   %s    %s ", CommandRunner.USER_ARG, user);
			final Args args = new Args(command);
			boolean host = args.hasArgument(CommandRunner.USER_ARG);
			assertTrue(host);
			assertEquals(user, args.getArgument(CommandRunner.USER_ARG));
		}

	}
	
	@Test
	public void PASSWORD_ARG() throws Exception {
		{
			final String command = String.format("run   %s  ", CommandRunner.PASSWORD_ARG);
			final Args args = new Args(command);
			boolean host = args.hasArgument(CommandRunner.PASSWORD_ARG);
			assertFalse(host);
		}
		{
			final String user = "xzxzxzx";
			final String command = String.format("run   %s    %s ", CommandRunner.PASSWORD_ARG, user);
			final Args args = new Args(command);
			boolean host = args.hasArgument(CommandRunner.PASSWORD_ARG);
			assertTrue(host);
			assertEquals(user, args.getArgument(CommandRunner.PASSWORD_ARG));
		}

	}
	
	
	
}

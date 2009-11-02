package com.jetbrains.teamcity.command;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;


public class CommandRunnerTest {

	private static CommandRunner ourRunner;

	@BeforeClass
	public static void setup() throws Exception {
		ourRunner = new CommandRunner();//register patterns  
	}	

	@Test
	public void TW_9949() {
		{
			final String defaultHost = CommandRunner.getDefaultHost();
			final String host = CommandRunner.getHost(new Args("run  -m  --host "));
			if(defaultHost == null){
				assertEquals(null, host);
			} else {
				assertEquals(defaultHost, host);
			}
		}
		{
			final String url = "http://1.2.3.4:8111";
			final String host = CommandRunner.getHost(new Args(String.format("   run    -m    --host   %s  ", url)));
			assertEquals(url, host);
		}
	}
	
}

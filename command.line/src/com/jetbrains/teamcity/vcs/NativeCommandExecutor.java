package com.jetbrains.teamcity.vcs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import org.apache.log4j.Logger;

public class NativeCommandExecutor {
	
	private static Logger LOGGER = Logger.getLogger(NativeCommandExecutor.class) ;	

	private static String ourNativeShellCommand;

	public static String execute(final String command, final File dir, String ...args) throws IOException {
		//add shell for pipe e.t.c support
		String nativeCommand = getNativeShellCommand() + " " + command.trim(); //$NON-NLS-1$
		for(String arg : args){
			nativeCommand += (" " + arg); //$NON-NLS-1$
		}
		LOGGER.debug(MessageFormat.format("execute \"{0}\" in \"{1}\"", nativeCommand, dir)); //$NON-NLS-1$
		try {
			final Process process = Runtime.getRuntime().exec(nativeCommand, null, dir);

			final int result = process.waitFor();
			
			if (result != 0) {
				final String err = readStream(process.getErrorStream());
				LOGGER.error(err);
				throw new RuntimeException(err);
				
			} else {
				final String out = readStream(process.getInputStream());
				LOGGER.debug(out);
				return out;
			}
		} catch (InterruptedException e) {
			LOGGER.debug(e);
			return "InterruptedException"; //$NON-NLS-1$
		}
	}

	private static String readStream(final InputStream stream)
			throws IOException {
		int c;
		StringBuffer buffer = new StringBuffer();
		while ((c = stream.read()) != -1) {
			buffer.append((char) c);
		}
		stream.close();
		return buffer.toString();
	}

	public static String[] execute(final String[] commands, final File dir)
			throws Exception {
		// TODO: implement it
		return null;
	}

	static String getNativeShellCommand() {
		if (ourNativeShellCommand == null) {
			final String osname = String.valueOf(System.getProperty("os.name")); //$NON-NLS-1$
			if (osname.toLowerCase().contains("windows")) { //$NON-NLS-1$
				ourNativeShellCommand = "cmd /c"; //$NON-NLS-1$
			} else {
				ourNativeShellCommand = "sh -c"; //$NON-NLS-1$
			}
		}
		return ourNativeShellCommand;
	}

}
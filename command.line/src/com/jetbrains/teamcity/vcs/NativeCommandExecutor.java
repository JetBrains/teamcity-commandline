package com.jetbrains.teamcity.vcs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import com.jetbrains.teamcity.Logger;

public class NativeCommandExecutor {

	private static String ourNativeShellCommand;

	public static String execute(final String command, final File dir, String ...args) throws IOException {
		
		//add shell for pipe e.t.c support
		String nativeCommand = getNativeShellCommand() + " " + command.trim();
		for(String arg : args){
			nativeCommand += (" " + arg);
		}
		Logger.log(NativeCommandExecutor.class.getName(), MessageFormat.format("execute \"{0}\" in \"{1}\"", nativeCommand, dir));
		try {
			final Process process = Runtime.getRuntime().exec(nativeCommand, null, dir);

			final int result = process.waitFor();
			
			if (result != 0) {
				final String err = readStream(process.getErrorStream());
				Logger.log(NativeCommandExecutor.class.getName(), err);
				throw new RuntimeException(err);
				
			} else {
				final String out = readStream(process.getInputStream());
				Logger.log(NativeCommandExecutor.class.getName(), out);
				return out;
			}
		} catch (InterruptedException e) {
			Logger.log(NativeCommandExecutor.class.getName(), e);
			return "InterruptedException";
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
			final String osname = String.valueOf(System.getProperty("os.name"));
			if (osname.toLowerCase().contains("windows")) {
				ourNativeShellCommand = "cmd /c";
			} else {
				ourNativeShellCommand = "sh -c";
			}
		}
		return ourNativeShellCommand;
	}

}
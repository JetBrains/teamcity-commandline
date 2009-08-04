package com.jetbrains.teamcity.runtime;

import java.io.PrintStream;


public class ConsoleProgressMonitor implements IProgressMonitor {

	private PrintStream myOut;
	private int myTaskWork;
	private Thread myCurrentTicker;

	public ConsoleProgressMonitor(PrintStream out) {
		myOut = out;
	}

	public IProgressMonitor beginTask(String name, int totalWork) {
		if (name != null && name.trim().length() > 0) {
			myOut.append(name).append("...");
			myTaskWork = totalWork;
			stopTicker();
			myCurrentTicker = new Thread(new Ticker(myOut), "Console Progress Monitor");
			myCurrentTicker.start();
		}
		return this;
	}

	private void stopTicker() {
		if(myCurrentTicker != null && myCurrentTicker.isAlive()){
			myCurrentTicker.interrupt();
		}
	}

	public IProgressMonitor done() {
		stopTicker();
		myOut.println("[done]");
		return this;
	}
	
	public IProgressMonitor done(String message) {
		if(message == null){
			done();
		} else {
			stopTicker();
			myOut.println(message);
		}
		return this;
	}	

	public IProgressMonitor worked(final String step) {
		if(step != null){
			myOut.print(step);
		}
		return this;
	}
	
	class Ticker implements Runnable {

		public Ticker(PrintStream out) {
			myOut = out;
		}

		public void run() {
			while (true) {
				try {
					Thread.sleep(1000);
					myOut.print(".");
				} catch (InterruptedException e) {
					return;
				}
			}
		}

	}

	
}
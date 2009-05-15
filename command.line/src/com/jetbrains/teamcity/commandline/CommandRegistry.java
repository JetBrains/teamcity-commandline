package com.jetbrains.teamcity.commandline;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jetbrains.buildServer.util.FileUtil;

import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Storage;
import com.jetbrains.teamcity.resources.VCSAccess;


class CommandRegistry {
	
	public static final Pattern COMMAND_EXTENSION_JAR_PATTERN = Pattern.compile(".*tc\\.command*");
	
	private static CommandRegistry ourInstance;
	
	private HashMap<String, ICommand> ourRegistry = new HashMap<String, ICommand>();
	
	private CommandRegistry(){
		//try to find any ICommand and register
		registerCommands();
	}

	void registerCommands() {
		//register default
		register(new Help());
		register(new List());
		register(new RemoteRun());
		register(new Share());
		register(new Unshare());		
		//scan for new
		//TODO: register found command
		if(getClass().getClassLoader() instanceof URLClassLoader){
			final URLClassLoader loader = (URLClassLoader) getClass().getClassLoader();
			for(URL url : loader.getURLs()){
				final File file = new File(url.getFile());
				if(file.isDirectory()){
					final ArrayList<File> children = new ArrayList<File>();
					FileUtil.collectMatchedFiles(file, Pattern.compile(".*\\.class"), children);
					for(File child : children){
						if(isSimpleClass(child.getPath())){//do not touch inner classes
							final String classFilePath = FileUtil.getRelativePath(file, child);
							final String className = getClassName(classFilePath);
							isCommand(className);
						}
					}
				} else {
					//jars
					System.err.println(file);
					try {
						final JarFile jar = new JarFile(file);
						if(!isIgnored(jar)){
							final Enumeration<JarEntry> entries = jar.entries();
							while(entries.hasMoreElements()){
								final String fileName = entries.nextElement().getName();
								if(isSimpleClass(fileName)){
									final String className = getClassName(fileName);
									isCommand(className);
								}
							}
						}
					} catch (IOException e) {
						//do nothing
					}
				}
			}
		}
	}

	private boolean isIgnored(JarFile jar) {
		Matcher matcher = COMMAND_EXTENSION_JAR_PATTERN.matcher(jar.getName());
		return !matcher.matches();
	}

	private boolean isCommand(final String className) {
		try {
			if(!isIgnored(className)){//do not instantiate self
				final Class<?> clazz = getClass().getClassLoader().loadClass(className);
				Object instance = clazz.newInstance();
				if(instance instanceof ICommand){
					return true;
				}
			}
		} catch (Throwable e) {
			//do nothing
		}
		return false;
	}

	private boolean isSimpleClass(final String fileName) {
		return fileName.endsWith(".class") && !fileName.contains("$");
	}

	private String getClassName(final String classFilePath) {
		return classFilePath.replace("\\", ".").replace("/", ".").substring(0, classFilePath.lastIndexOf(".class"));
	}

	private boolean isIgnored(final String className) {
		if (getClass().getName().equals(className)
				|| VCSAccess.class.getName().equals(className)
				|| Storage.class.getName().equals(className)
				|| Server.class.getName().equals(className)
				|| CommandRunner.class.getName().equals(className)) {
			return true;
		}
		return false;
	}

	public static synchronized CommandRegistry getInstance(){
		if(ourInstance == null){
			ourInstance = new CommandRegistry();
		}
		return ourInstance;
	}
	
	synchronized void register(final ICommand command){
		if(!ourRegistry.containsKey(command.getId())){
			ourRegistry.put(command.getId(), command);
		}
	}
	
	synchronized ICommand getCommand(final String id){
		if(!ourRegistry.containsKey(id)){
			return ourRegistry.get(Help.ID);
		}
		return ourRegistry.get(id);
	}

}

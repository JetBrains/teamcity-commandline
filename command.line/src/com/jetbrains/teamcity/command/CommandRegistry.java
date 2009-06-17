package com.jetbrains.teamcity.command;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jetbrains.buildServer.util.FileUtil;

import com.jetbrains.teamcity.Logger;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Storage;
import com.jetbrains.teamcity.command.CommandRegistry;
import com.jetbrains.teamcity.command.CommandRunner;
import com.jetbrains.teamcity.command.Help;
import com.jetbrains.teamcity.command.ICommand;
import com.jetbrains.teamcity.command.List;
import com.jetbrains.teamcity.command.Login;
import com.jetbrains.teamcity.command.Logout;
import com.jetbrains.teamcity.command.RemoteRun;
import com.jetbrains.teamcity.command.Share;
import com.jetbrains.teamcity.command.Unshare;
import com.jetbrains.teamcity.resources.TCAccess;


class CommandRegistry {
	
	public static final Pattern COMMAND_EXTENSION_JAR_PATTERN = Pattern.compile(".*tc\\.command.*");
	
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
		register(new Login());
		register(new Logout());
		register(new RemoteRun());
		register(new Share());
		register(new Unshare());		
		//scan for new
		final Collection<ICommand> extensions = findCommand();
		for(final ICommand command : extensions){
			register(command);
		}
	}
	
	Collection<ICommand> commands(){
		return Collections.<ICommand>unmodifiableCollection(ourRegistry.values());
	}

	private Collection<ICommand> findCommand() {
		final HashSet<ICommand> result = new HashSet<ICommand>();
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
							final ICommand instance = createCommand(className);
							if(instance != null){
								result.add(instance);
							}
						}
					}
				} else {
					//jars
					try {
						final JarFile jar = new JarFile(file);
						if(!isCommandExtensionJar(jar.getName())){
							Logger.log(CommandRegistry.class.getName(), MessageFormat.format("New commans jar found: {0}", jar.getName()));
							final Enumeration<JarEntry> entries = jar.entries();
							while(entries.hasMoreElements()){
								final String fileName = entries.nextElement().getName();
								if(isSimpleClass(fileName)){
									final String className = getClassName(fileName);
									final ICommand instance = createCommand(className);
									if(instance != null){
										result.add(instance);
									}
								}
							}
						}
					} catch (IOException e) {
						//do nothing
					}
				}
			}
		}
		return result;
	}

	boolean isCommandExtensionJar(final String jarName) {
		Matcher matcher = COMMAND_EXTENSION_JAR_PATTERN.matcher(jarName);
		return !matcher.matches();
	}

	private ICommand createCommand(final String className) {
		try {
			if(!isIgnored(className)){//do not instantiate self
				final Class<?> clazz = getClass().getClassLoader().loadClass(className);
				Object instance = clazz.newInstance();
				if(instance instanceof ICommand){
					return (ICommand) instance;
				}
			}
		} catch (Throwable e) {
			//do nothing
		}
		return null;
	}

	private boolean isSimpleClass(final String fileName) {
		return fileName.endsWith(".class") && !fileName.contains("$");
	}

	private String getClassName(final String classFilePath) {
		return classFilePath.replace("\\", ".").replace("/", ".").substring(0, classFilePath.lastIndexOf(".class"));
	}

	private boolean isIgnored(final String className) {
		if (getClass().getName().equals(className)
				|| TCAccess.class.getName().equals(className)
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
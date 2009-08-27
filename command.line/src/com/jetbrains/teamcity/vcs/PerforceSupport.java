package com.jetbrains.teamcity.vcs;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.log4j.Logger;

import com.jetbrains.teamcity.ECommunicationException;

public class PerforceSupport {
	
	private static Logger LOGGER = Logger.getLogger(PerforceSupport.class) ;
	
	private static final String NEWLINE_PATTERN = "[\n\r]";
	private static final String SPACE = "\\s+"; //$NON-NLS-1$
	private static final String SLASH = "/"; //$NON-NLS-1$
	
	static final String TEAM_CITY_AGENT = "//team-city-agent/"; //$NON-NLS-1$
	static final String TEAM_CITY_AGENT_ROOT = "//team-city-agent/..."; //$NON-NLS-1$
	
	private static final String USE_CLIENT = "use-client"; //$NON-NLS-1$
	private static final String CLIENT_MAPPING = "client-mapping"; //$NON-NLS-1$

	public static String findPerforceRoot(final Map<String, String> properties) {
		final String useClient = properties.get(USE_CLIENT);
		if(!Boolean.TRUE.equals(Boolean.parseBoolean(useClient))){
			final String clientMapping = properties.get(CLIENT_MAPPING); //$NON-NLS-1$
			if(clientMapping != null && clientMapping.trim().length() != 0){
				final String uniqueRoot = findPerforceRoot(clientMapping);
				if(uniqueRoot != null){
					return uniqueRoot;
				}
			}
		}
		return null;
	}
	
	/**
	 * looking for unique string line contains "//team-city-agent/" token
	 */
	static String findPerforceRoot(final String clientMapping) {
		try{
			final String[] lines = clientMapping.split(NEWLINE_PATTERN);
			if(lines.length == 1){
				final String[] columns = lines[0].split(SPACE);
				final String root = columns[0].trim();
				return root.substring(0, root.lastIndexOf(SLASH));
			} else {
				String root = null;
				for(String line : lines){
					final String[] columns = line.split(SPACE);
					if (columns.length > 1) {
						if(columns[1].trim().equalsIgnoreCase(TEAM_CITY_AGENT_ROOT)){
							return columns[0].trim().substring(0, columns[0].lastIndexOf(SLASH));
						} else if (columns[1].toLowerCase().startsWith(TEAM_CITY_AGENT)){
							if(root == null){
								root = columns[0].trim();
							} else {
								return null; //there are any entries for mapping 
							}
						}
					}
				}
				if(root!=null){
					return root.substring(0, root.lastIndexOf(SLASH));
				}
			}
		} catch (Exception e){
			LOGGER.error(MessageFormat.format("Unexpected error over \"{0}\" ClientMapping parsing", clientMapping), e);
		}
		return null;
	}

	public static String getRepositoryPath(final String port, final String client, final String user, final String password, final File file) throws ECommunicationException {
		//prepare command
		final StringBuffer result = new StringBuffer("p4 -p " + port);
		if(client != null){
			result.append(" -c ").append(client);
		}
		if(user != null){
			result.append(" -u ").append(user);
			LOGGER.debug(MessageFormat.format("$P4USER set to {0}", user));
		}
		if(password != null){
			result.append(" -P ").append(password);
			LOGGER.debug(MessageFormat.format("$P4PASSWD set to {0}", password));
		}
		result.append(" where ");
		//run p4
		try{
			final String[] where = NativeCommandExecutor.execute(result.toString() + file.getAbsolutePath(), file.getParentFile()).trim().split(SPACE);
			if(where.length > 0){
				return where[0];
			}
		} catch (Throwable t){
			throw new ECommunicationException(MessageFormat.format("Check your $P4PORT, $P4CLIENT, $P4USER or $P4PASSWD.\nP4 raw message: {0}", t.getMessage()), t);
		}
		return null;
	}

}

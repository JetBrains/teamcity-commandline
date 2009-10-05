package com.jetbrains.teamcity.resources;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import jetbrains.buildServer.serverSide.crypt.EncryptUtil;

import org.apache.log4j.Logger;

import com.jetbrains.teamcity.Storage;

public class TCAccess {
	
	private static Logger LOGGER = Logger.getLogger(TCAccess.class) ;
	
	static Storage.IKey<ArrayList<ICredential>> CREDENTIAL_KEY = new Storage.IKey<ArrayList<ICredential>>(){
		
		private static final long serialVersionUID = 6509717225043443905L;

		public Object getKey() {
			return TCAccess.class.getName() + ".CREDENTIAL"; //$NON-NLS-1$
		}
	};
	

	private static TCAccess ourInstance;
	
	private ArrayList<ICredential> myCredentials;

	public static synchronized TCAccess getInstance(){
		if(ourInstance == null){
			ourInstance = new TCAccess();
		}
		return ourInstance;
	}

	private TCAccess(){
		//credentials. note: password encoded
		final ArrayList<ICredential> credentials = Storage.getInstance().get(CREDENTIAL_KEY);
		myCredentials = new ArrayList<ICredential>();
		if(credentials != null){
			myCredentials.addAll(credentials);
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		Storage.getInstance().flush();
		LOGGER.debug("Flush enforced");
		super.finalize();
	}

	synchronized void clear(){
		myCredentials.clear();
		Storage.getInstance().put(CREDENTIAL_KEY, new ArrayList<ICredential>(myCredentials), true);
	}
	
	static class TeamCityCredential implements ICredential, Serializable {

		private static final long serialVersionUID = 468772737075052773L;
		
		private String myPassword;
		private String myServer;
		private String myUser;
		private long myTs;
		
		TeamCityCredential(final String url, final String user, final String password){
			myPassword = password;//TODO: crypt it
			myServer = url;
			myUser = user;
			myTs = System.currentTimeMillis();
		}

		public String getPassword() {
			return myPassword;
		}

		public String getServer() {
			return myServer;
		}

		public String getUser() {
			return myUser;
		}
		
		public String toString() {
			return MessageFormat.format("{0}:{1}:*************", myServer, myUser, myPassword); //$NON-NLS-1$
		}

		public long getCreationTimestamp() {
			return myTs;
		}
		
	}
	
	public ICredential findCredential(final String host){
		for(ICredential credential : myCredentials){
			try {
				if(new URL(credential.getServer()).equals(new URL(host))){
					return credential;
				}
			} catch (MalformedURLException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return null;
	}
	
	public void setCredential(final String url, final String user, final String password){
		final String scrambled = EncryptUtil.scramble(password);
		final TeamCityCredential account = new TeamCityCredential(url, user, scrambled);//make serializable
		//check
		try{
			final ICredential existing = findCredential(account.getServer());
			if(existing == null){
				myCredentials.add(account);
			} else {
				myCredentials.remove(existing);
				myCredentials.add(account);
			}
		} finally {
			Storage.getInstance().put(CREDENTIAL_KEY, new ArrayList<ICredential>(myCredentials), true);
		}
	}
	
	public void removeCredential(final String host){
		//check
		try{
			final ICredential existing = findCredential(host);
			if(existing != null){
				myCredentials.remove(existing);
			}
		} finally {
			Storage.getInstance().put(CREDENTIAL_KEY, new ArrayList<ICredential>(myCredentials), true);
		}
	}
	
	public Collection<ICredential> credentials(){
		return Collections.<ICredential>unmodifiableCollection(myCredentials); //do not allow modification
	}
	
}

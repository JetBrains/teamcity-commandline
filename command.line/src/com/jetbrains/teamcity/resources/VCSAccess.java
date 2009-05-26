package com.jetbrains.teamcity.resources;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

import jetbrains.buildServer.vcs.VcsRoot;

import com.jetbrains.teamcity.Logger;
import com.jetbrains.teamcity.Storage;

public class VCSAccess {
	
	static Storage.IKey<Long> SHARES_COUNTER_KEY = new Storage.IKey<Long>(){

		private static final long serialVersionUID = 6509717225043443905L;

		@Override
		public Object getKey() {
			return VCSAccess.class.getName() + ".SHARESCOUNTER";
		}
		
	};
	
	static Storage.IKey<ArrayList<IVCSRoot>> SHARES_KEY = new Storage.IKey<ArrayList<IVCSRoot>>(){
		
		private static final long serialVersionUID = 6509717225043443905L;

		@Override
		public Object getKey() {
			return VCSAccess.class.getName() + ".SHARES";
		}
	
	};
	
	static Storage.IKey<ArrayList<ICredential>> CREDENTIAL_KEY = new Storage.IKey<ArrayList<ICredential>>(){
		
		private static final long serialVersionUID = 6509717225043443905L;

		@Override
		public Object getKey() {
			return VCSAccess.class.getName() + ".CREDENTIAL";
		}
	};
	

	private static VCSAccess ourInstance;
	
	private ArrayList<IVCSRoot> myShares;
	
	private ArrayList<ICredential> myCredentials;

	public static synchronized VCSAccess getInstance(){
		if(ourInstance == null){
			ourInstance = new VCSAccess();
		}
		return ourInstance;
	}

	private VCSAccess(){
		//roots
		final Collection<IVCSRoot> shares = Storage.getInstance().get(SHARES_KEY);
		if(shares != null){
			myShares = new ArrayList<IVCSRoot>(shares);
		} else {
			myShares = new ArrayList<IVCSRoot>();
		}
		//credentials
		ArrayList<ICredential> credentials = Storage.getInstance().get(CREDENTIAL_KEY);
		if(credentials != null){
			myCredentials = new ArrayList<ICredential>(credentials);
		} else {
			myCredentials = new ArrayList<ICredential>();
		}
		
	}
	
	public Collection<IVCSRoot> roots(){
		return Collections.<IVCSRoot>unmodifiableCollection(myShares); //do not allow modification
	}
	
	public IVCSRoot getRoot(String forPath) throws IllegalArgumentException {
		try {
			forPath = new File(forPath.trim()).getCanonicalPath();
			//make sure the deepest candidate will the last
			final TreeSet<IVCSRoot> rootCandidate = new TreeSet<IVCSRoot>(new Comparator<IVCSRoot>(){
				@Override
				public int compare(IVCSRoot o1, IVCSRoot o2) {
					return o1.getLocal().toLowerCase().compareTo(o2.getLocal().toLowerCase());
				}});
			//scan all to get all hierarchial roots
			for(final IVCSRoot root : myShares){
				final String existsRootPath = root.getLocal();
				String parentPath = forPath.toLowerCase();
				while(parentPath != null){
					if(parentPath.equalsIgnoreCase(existsRootPath)){
						rootCandidate.add(root);
					}
					parentPath = new File(parentPath).getParent();
					if(parentPath!= null){
						parentPath = parentPath.toLowerCase();
					}
				}
			}
			if(!rootCandidate.isEmpty()){
				return rootCandidate.last();
			}
			return null;
			
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public synchronized IVCSRoot share(String localRoot, final VcsRoot remote) throws IllegalArgumentException {
		try {
			localRoot = new File(localRoot).getCanonicalPath();
			//check exists
			validate(localRoot);
			//generate new share id
			Long lastShareId = Storage.getInstance().get(SHARES_COUNTER_KEY);
			if(lastShareId == null){
				lastShareId = 0l;
			}
			//create new and persist
			final Long newShareID = lastShareId + 1; 
			final TeamCityRoot newRoot = new TeamCityRoot(newShareID.toString(), localRoot, remote);
			myShares.add(newRoot);
			Storage.getInstance().put(SHARES_KEY, myShares, false);
			Storage.getInstance().put(SHARES_COUNTER_KEY, newShareID);
			return newRoot;
			
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	

	public synchronized void unshare(final String id) throws IllegalArgumentException {
		for(IVCSRoot root : roots()){
			if(id.equals(root.getId())){
				myShares.remove(root);
				Storage.getInstance().put(SHARES_KEY, myShares);
			}
			Logger.log(VCSAccess.class.getName(), MessageFormat.format("Share \"{0}\" succesfully removed", id));
			return;
		}
		throw new IllegalArgumentException(MessageFormat.format("Could not find share \"{0}\"", id));  
	}
	
	synchronized void clear(){
		myShares.clear();
		myCredentials.clear();
		Storage.getInstance().put(SHARES_KEY, myShares, false);
		Storage.getInstance().put(CREDENTIAL_KEY, myCredentials, true);
	}
	
	private void validate(String localRoot) throws IllegalArgumentException {
		final File file = new File(localRoot);
		//check exists
		if(!file.exists()){
			throw new IllegalArgumentException(MessageFormat.format("Path is not found: {0}", localRoot));
		}
		//check isFolder
		if(!file.isDirectory()){
			throw new IllegalArgumentException(MessageFormat.format("Only folder can be shared: {0}", localRoot));
		}
	}
	
	
	static class TeamCityRoot implements IVCSRoot, Serializable {
		
		private static final long serialVersionUID = -5096247011911645590L;
		
		private Long myRemote;
		private String myLocal;
		private String myId;
		private Map<String, String> myProperies;

		private String myVcs;

		TeamCityRoot(final String id, final String local, final VcsRoot remote){
			myId = id;
			myLocal = local;
			myRemote = remote.getId();
			myVcs = remote.getVcsName();
			myProperies = Collections
					.unmodifiableMap(remote.getProperties() == null ? 
							Collections.<String, String> emptyMap() : 
							remote.getProperties());
		}
		
		@Override
		public String toString() {
			return MessageFormat.format("id={0}, local={1}, remote={2}", getId(), getLocal(), getRemote());
		}

		@Override
		public boolean equals(Object obj) {
			if(obj instanceof IVCSRoot){
				return this.myLocal.equals(((IVCSRoot)obj).getLocal()) && this.myRemote.equals(((IVCSRoot)obj).getRemote());
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return this.myLocal.hashCode() << 6 | this.myRemote.hashCode();
		}

		@Override
		public String getLocal() {
			return myLocal;
		}

		@Override
		public Long getRemote() {
			return myRemote;
		}

		@Override
		public String getId() {
			return myId;
		}

		@Override
		public Map<String, String> getProperties() {
			return myProperies;
		}

		@Override
		public String getVcs() {
			return myVcs;
		}
		
	}
	
	static class TeamCityCredential implements ICredential, Serializable {

		private static final long serialVersionUID = 468772737075052773L;
		
		private String myPassword;
		private String myServer;
		private String myUser;
		
		TeamCityCredential(final String url, final String user, final String password){
			myPassword = password;//TODO: crypt it
			myServer = url;
			myUser = user;
		}

		@Override
		public String getPassword() {
			return myPassword;
		}

		@Override
		public String getServer() {
			return myServer;
		}

		@Override
		public String getUser() {
			return myUser;
		}
		
		@Override
		public String toString() {
			return MessageFormat.format("{0}:{1}:*************", myServer, myUser, myPassword);
		}
		
	}
	
	public ICredential findCredential(final String host){
		for(ICredential credential : myCredentials){
			try {
				if(new URL(credential.getServer()).equals(new URL(host))){
					return credential;
				}
			} catch (MalformedURLException e) {
				Logger.log(VCSAccess.class.getName(), e);
			}
		}
		return null;
	}
	
	public void setCredential(final String url, final String user, final String password){
		final TeamCityCredential account = new TeamCityCredential(url, user, password);//make serializable
		//check
		try{
			final ICredential existing = findCredential(account.getServer());
			if(existing == null){
				myCredentials.add(account);
			} else {
				Logger.log(VCSAccess.class.getName(), MessageFormat.format("will replace existing credential \"{0}\" with new one \"{1}\"", String.valueOf(existing), String.valueOf(account)));
				myCredentials.remove(existing);
				myCredentials.add(account);
			}
		} finally {
			Storage.getInstance().put(CREDENTIAL_KEY, myCredentials, true);
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
			Storage.getInstance().put(CREDENTIAL_KEY, myCredentials, true);
		}
	}
	
	public Collection<ICredential> credentials(){
		return Collections.<ICredential>unmodifiableCollection(myCredentials); //do not allow modification
	}
	
	
}

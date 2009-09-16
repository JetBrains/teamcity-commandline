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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import jetbrains.buildServer.serverSide.crypt.EncryptUtil;
import jetbrains.buildServer.vcs.VcsRoot;

import org.apache.log4j.Logger;

import com.jetbrains.teamcity.Storage;
import com.jetbrains.teamcity.Util;

public class TCAccess {
	
	private static Logger LOGGER = Logger.getLogger(TCAccess.class) ;
	
	static Storage.IKey<Long> SHARES_COUNTER_KEY = new Storage.IKey<Long>(){

		private static final long serialVersionUID = 6509717225043443905L;

		public Object getKey() {
			return TCAccess.class.getName() + ".SHARESCOUNTER"; //$NON-NLS-1$
		}
		
	};
	
	static Storage.IKey<ArrayList<IShare>> SHARES_KEY = new Storage.IKey<ArrayList<IShare>>(){
		
		private static final long serialVersionUID = 6509717225043443905L;

		public Object getKey() {
			return TCAccess.class.getName() + ".SHARES"; //$NON-NLS-1$
		}
	
	};
	
	static Storage.IKey<HashMap<String, String>> SHARE_DEFAULT_MAPPING_KEY = new Storage.IKey<HashMap<String, String>>(){
		
		private static final long serialVersionUID = 6509717225043443905L;

		public Object getKey() {
			return TCAccess.class.getName() + ".SHARE_DEFAULT_MAPPING"; //$NON-NLS-1$
		}
	
	};
	
	static Storage.IKey<ArrayList<ICredential>> CREDENTIAL_KEY = new Storage.IKey<ArrayList<ICredential>>(){
		
		private static final long serialVersionUID = 6509717225043443905L;

		public Object getKey() {
			return TCAccess.class.getName() + ".CREDENTIAL"; //$NON-NLS-1$
		}
	};
	

	private static TCAccess ourInstance;
	
	private ArrayList<IShare> myShares;
	
	private ArrayList<ICredential> myCredentials;

	private HashMap<String, String> myMappings;

	public static synchronized TCAccess getInstance(){
		if(ourInstance == null){
			ourInstance = new TCAccess();
		}
		return ourInstance;
	}

	private TCAccess(){
		//roots
		final Collection<IShare> shares = Storage.getInstance().get(SHARES_KEY);
		if(shares != null){
			myShares = new ArrayList<IShare>(shares);
		} else {
			myShares = new ArrayList<IShare>();
		}
		//credentials. note: password encoded
		final ArrayList<ICredential> credentials = Storage.getInstance().get(CREDENTIAL_KEY);
		myCredentials = new ArrayList<ICredential>();
		if(credentials != null){
			myCredentials.addAll(credentials);
		}
		//default mappings
		final HashMap<String, String> mappings = Storage.getInstance().get(SHARE_DEFAULT_MAPPING_KEY);
		myMappings = new HashMap<String, String>();
		if(mappings != null){
			myMappings.putAll(mappings);
		}
		
	}
	
	@Deprecated
	Collection<IShare> roots(){
		return Collections.<IShare>unmodifiableCollection(myShares); //do not allow modification
	}
	
	@Deprecated
	IShare getRoot(String forPath) throws IllegalArgumentException {
		try {
			final String absolutePath = new File(Util.toPortableString(forPath.trim())).getCanonicalFile().getAbsolutePath();
			forPath = Util.toPortableString(absolutePath);
			//make sure the deepest candidate will the last
			final TreeSet<IShare> rootCandidate = new TreeSet<IShare>(new Comparator<IShare>(){
				
				public int compare(IShare o1, IShare o2) {
					return Util.toPortableString(o1.getLocal().toLowerCase()).compareTo(Util.toPortableString(o2.getLocal().toLowerCase()));
				}});
			//scan all to get all hierarchial roots
			for(final IShare root : myShares){
				final String existsRootPath = Util.toPortableString(root.getLocal());
				String parentPath = Util.toPortableString(forPath.toLowerCase().trim());
				while(parentPath != null){
					if(parentPath.equalsIgnoreCase(existsRootPath)){
						rootCandidate.add(root);
					}
					parentPath = new File(parentPath).getParent();
					if(parentPath!= null){
						parentPath = Util.toPortableString(parentPath.toLowerCase());
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
	
	@Deprecated
	synchronized IShare share(String localRoot, final VcsRoot remote) throws IllegalArgumentException {
		try {
			localRoot = new File(Util.toPortableString(localRoot)).getCanonicalFile().getAbsolutePath();
			//check exists
			validate(localRoot);
			//TODO: extract method
			//remove the same local roots 
			final Iterator<IShare> iterator = myShares.iterator();
			while(iterator.hasNext()){
				final IShare share = iterator.next();
				if(share.getLocal().equals(localRoot)){
					unshare(share.getId());
					break;
				}
			}
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
			
			LOGGER.debug(MessageFormat.format("New Share \"{0}\" is created: {1}->{2}", newRoot.myId, newRoot.myLocal, newRoot.myRemote)); //$NON-NLS-1$
			return newRoot;
			
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	@Deprecated
	void update(IShare share, VcsRoot root) {
		if(share instanceof TeamCityRoot){
			((TeamCityRoot)share).update(root.getProperties());
			Storage.getInstance().put(SHARES_KEY, myShares);
		} else {
			throw new IllegalArgumentException("Could not update Share {0}: read-only"); //$NON-NLS-1$
		}
	}

	@Override
	protected void finalize() throws Throwable {
		Storage.getInstance().flush();
		LOGGER.debug("Flush enforced");
		super.finalize();
	}

	
	@Deprecated
	synchronized void unshare(final String id) throws IllegalArgumentException {
		for(IShare root : roots()){
			if(id.equals(root.getId())){
				myShares.remove(root);
				Storage.getInstance().put(SHARES_KEY, myShares);
				LOGGER.debug(MessageFormat.format("Share \"{0}\" succesfully removed", id)); //$NON-NLS-1$
				removeDefaultMapping(root);
				return;
			}
		}
		throw new IllegalArgumentException(MessageFormat.format("Could not find share \"{0}\"", id));   //$NON-NLS-1$
	}
	
	synchronized void clear(){
		myShares.clear();
		myCredentials.clear();
		myMappings.clear();
		Storage.getInstance().put(SHARES_KEY, myShares, false);
		Storage.getInstance().put(SHARE_DEFAULT_MAPPING_KEY, myMappings, false);
		Storage.getInstance().put(CREDENTIAL_KEY, new ArrayList<ICredential>(myCredentials), true);
	}
	
	private void validate(String localRoot) throws IllegalArgumentException {
		final File file = new File(localRoot);
		//check exists
		if(!file.exists()){
			throw new IllegalArgumentException(MessageFormat.format("Path is not found: {0}", localRoot)); //$NON-NLS-1$
		}
		//check isFolder
		if(!file.isDirectory()){
			throw new IllegalArgumentException(MessageFormat.format("Only folder can be shared: {0}", localRoot)); //$NON-NLS-1$
		}
	}
	
	@Deprecated
	static class TeamCityRoot implements IShare, Serializable {
		
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
			return MessageFormat.format("local={0}, remote={1}, vcs={2}, properties={3}",  //$NON-NLS-1$
					getLocal(), getRemote(), getVcs(), getProperties());
		}

		@Override
		public boolean equals(Object obj) {
			if(obj instanceof IShare){
				return this.myLocal.equals(((IShare)obj).getLocal()) && this.myRemote.equals(((IShare)obj).getRemote());
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return this.myLocal.hashCode() << 6 | this.myRemote.hashCode();
		}

		public String getLocal() {
			return myLocal;
		}

		public Long getRemote() {
			return myRemote;
		}

		public String getId() {
			return myId;
		}

		public Map<String, String> getProperties() {
			return myProperies;
		}

		public String getVcs() {
			return myVcs;
		}

		public void update(Map<String, String> properties) {
			myProperies = properties;
		}
		
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

	@Deprecated
	void setDefaultMapping(final IShare share, String mapping) {
		myMappings.put(share.getId(), mapping);
		Storage.getInstance().put(SHARE_DEFAULT_MAPPING_KEY, myMappings);		
	}

	@Deprecated
	String getDefaultMapping(final IShare share) {
		return myMappings.get(share.getId());
	}

	@Deprecated
	void removeDefaultMapping(final IShare share) {
		final String shareId = share.getId();
		if(myMappings.containsKey(shareId)){
			LOGGER.debug(MessageFormat.format("Default mapping \"{0}\" succesfully removed", myMappings.get(shareId))); //$NON-NLS-1$
			myMappings.remove(shareId);
			Storage.getInstance().put(SHARE_DEFAULT_MAPPING_KEY, myMappings);
		}
	}
	
	
}

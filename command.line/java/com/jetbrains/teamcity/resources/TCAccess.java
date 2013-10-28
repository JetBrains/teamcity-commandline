/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.teamcity.resources;

import com.jetbrains.teamcity.Debug;
import com.jetbrains.teamcity.Storage;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import jetbrains.buildServer.serverSide.crypt.EncryptUtil;

public class TCAccess {

  static Storage.IKey<ArrayList<ICredential>> CREDENTIAL_KEY = new Storage.IKey<ArrayList<ICredential>>() {

    private static final long serialVersionUID = 6509717225043443905L;

    public Object getKey() {
      return TCAccess.class.getName() + ".CREDENTIAL"; //$NON-NLS-1$
    }
  };

  static Storage.IKey<ArrayList<?>> SHARES_KEY = new Storage.IKey<ArrayList<?>>() {

    private static final long serialVersionUID = 6509717225043443905L;

    public Object getKey() {
      return TCAccess.class.getName() + ".SHARES"; //$NON-NLS-1$
    }

  };

  private static TCAccess ourInstance;

  private final ArrayList<ICredential> myCredentials;

  public static synchronized TCAccess getInstance() {
    if (ourInstance == null) {
      ourInstance = new TCAccess();
    }
    return ourInstance;
  }

  private TCAccess() {
    // //cleanup
    // Storage.getInstance().remove(SHARES_KEY);
    // credentials. note: password encoded
    final ArrayList<ICredential> credentials = Storage.getInstance().get(CREDENTIAL_KEY);
    myCredentials = new ArrayList<ICredential>();
    if (credentials != null) {
      myCredentials.addAll(credentials);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    Storage.getInstance().flush();
    Debug.getInstance().debug(TCAccess.class, "Flush enforced");
    super.finalize();
  }

  synchronized void clear() {
    myCredentials.clear();
    Storage.getInstance().put(CREDENTIAL_KEY, new ArrayList<ICredential>(myCredentials), true);
  }

  static class TeamCityCredential implements ICredential, Serializable {

    private static final long serialVersionUID = 468772737075052773L;

    private final String myPassword;
    private final String myServer;
    private final String myUser;
    private final long myTs;

    TeamCityCredential(final String url, final String user, final String password) {
      myPassword = password;// TODO: crypt it
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

  public ICredential findCredential(final String host) {
    for (ICredential credential : myCredentials) {
      try {
        if (new URL(credential.getServer()).equals(new URL(host))) {
          return credential;
        }
      } catch (MalformedURLException e) {
        Debug.getInstance().error(TCAccess.class, e.getMessage(), e);
      }
    }
    return null;
  }

  public void setCredential(final String url, final String user, final String password) {
    final String scrambled = EncryptUtil.scramble(password);
    final TeamCityCredential account = new TeamCityCredential(url, user, scrambled);// make
                                                                                    // serializable
    // check
    try {
      final ICredential existing = findCredential(account.getServer());
      if (existing == null) {
        myCredentials.add(account);
      } else {
        myCredentials.remove(existing);
        myCredentials.add(account);
      }
    } finally {
      Storage.getInstance().put(CREDENTIAL_KEY, new ArrayList<ICredential>(myCredentials), true);
    }
  }

  public void removeCredential(final String host) {
    // check
    try {
      final ICredential existing = findCredential(host);
      if (existing != null) {
        myCredentials.remove(existing);
      }
    } finally {
      Storage.getInstance().put(CREDENTIAL_KEY, new ArrayList<ICredential>(myCredentials), true);
    }
  }

  public Collection<ICredential> credentials() {
    return Collections.unmodifiableCollection(myCredentials); // do
                                                                            // not
                                                                            // allow
                                                                            // modification
  }

  // TODO: remove next release & uncomment "cleanup"
  @Deprecated
  public static class TeamCityRoot implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long myRemote;
    private String myLocal;
    private String myId;
    private Map<String, String> myProperies;
    private String myVcs;

  }

}

package com.jetbrains.teamcity;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import jetbrains.BuildServerCreator;
import jetbrains.buildServer.serverProxy.RemoteAuthenticationServer;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.impl.XmlRpcSessionManager;
import jetbrains.buildServer.serverSide.impl.auth.XmlRpcAuthenticationServer;
import jetbrains.buildServer.users.SUser;
import org.apache.xmlrpc.WebServer2;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class ServerFunctionalTest extends BaseServerTestCase {
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";

  private SUser myUser;
  private WebServer2 myWebServer;
  private ServerFacadeWrapper myServerFacade;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();

    myUser = createUser(USERNAME);
    myUser.setPassword(PASSWORD);

    final XmlRpcSessionManager xmlRpcSessionManager = new XmlRpcSessionManager(myServer.getExecutor());
    final String sessionId = BuildServerCreator.startNewSession(xmlRpcSessionManager, null).getSessionId();
    assertNotNull(sessionId);
    final XmlRpcAuthenticationServer authServer = new XmlRpcAuthenticationServer(myServer, myFixture.getServerLoginModel(), xmlRpcSessionManager);

    myWebServer = BuildServerCreator.createAndStartXmlRpcServer();
    myWebServer.addHandler(RemoteAuthenticationServer.REMOTE_AUTH_SERVER, BuildServerCreator.makeSessionInXmlRpcHandler(sessionId, xmlRpcSessionManager, RemoteAuthenticationServer.class, authServer));

    // run tcc in separate class loader to not use TeamCity libs
    final ClassLoader tccClassLoader = new URLClassLoader(new URL[] { new File("../tmp/tcc.jar").getAbsoluteFile().toURI().toURL() }, null); // set parent to null, it's important to not pass TeamCity jars!
    myServerFacade = new ServerFacadeWrapper(tccClassLoader, new URL("http", myWebServer.getAddress().getHostAddress(), myWebServer.getPort(), ""));
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    myWebServer.shutdown();
    super.tearDown();
  }

  @Test
  public void test_login() throws Exception {
    myServerFacade.connect();
    myServerFacade.logon(USERNAME, PASSWORD);
    assertEquals(myUser.getId(), myServerFacade.getCurrentUser());
  }
}

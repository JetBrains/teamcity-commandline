package jetbrains.buildServer.commandline;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.serverSide.auth.RoleScope;
import jetbrains.buildServer.serverSide.impl.BuildTypeImpl;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.vcs.VcsClientMapping;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.openapi.WebResourcesManager;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.Test;

/**
 * @author kir
 */
@Test
public class CommandLineControllerTest extends BaseControllerTestCase {
  @Override
  protected BaseController createController(final WebControllerManager manager, final WebResourcesManager resourcesManager) {

    return new CommandLineController(mockPluginDescriptor(), manager, myFixture.getProjectManager(), 
                                     myServer.getVcsManager(), myFixture.getSecurityContext());
  }

  private PluginDescriptor mockPluginDescriptor() {
    Mockery context = new Mockery();
    final PluginDescriptor descriptor = context.mock(PluginDescriptor.class);
    context.checking(new Expectations() {{
      allowing(descriptor).getPluginName(); will(returnValue("cmdline"));
      allowing(descriptor).getPluginResourcesPath(); will(returnValue("/plugins/cmdline/"));
      allowing(descriptor).getPluginResourcesPath(with(any(String.class))); will(returnValue("/plugins/cmdline/"));
    }});
    return descriptor;
  }

  public void should_fill_the_model() throws Exception {

    processRequest();
    
    assertEquals(0, ((List)getModel().get("buildTypes")).size());
    assertEquals("/buildServer1234/plugins/cmdline/", getModel().get("cmdPathPrefix"));
  }


  public void no_right_to_fill_buildTypes_with_vcs_roots() throws Exception {

    final SUser user = createUser("kir");
    makeLoggedIn(user);

    myFixture.addVcsRoot("badSvn", "");

    addBuildTypeWithVcsMapping();

    processRequest();
    
    assertEquals("No right Permission.VIEW_BUILD_CONFIGURATION_SETTINGS for build type", 0, ((List)getModel().get("buildTypes")).size());
  }

  public void should_fill_buildTypes_with_vcs_roots() throws Exception {

    final SUser user = createUser("kir");
    makeLoggedIn(user);

    myFixture.addVcsRoot("badSvn", "");

    final BuildTypeImpl goodBuildType = addBuildTypeWithVcsMapping();

    addRole2LoggedInUser(RoleScope.projectScope(goodBuildType.getProjectId()), getProjectDevRole());

    processRequest();

    assertEquals(1, ((List)getModel().get("buildTypes")).size());
  }

  private BuildTypeImpl addBuildTypeWithVcsMapping() {
    PathPrefixesSupport.registerVcsRootMappingSupport(new ArrayList<VcsClientMapping>(), myServer.getVcsManager() );
    final BuildTypeImpl goodBuildType = registerBuildType("supportedBuildType", "ddd", "Ant");
    myFixture.addVcsRoot("mock", "", goodBuildType);
    return goodBuildType;
  }
}

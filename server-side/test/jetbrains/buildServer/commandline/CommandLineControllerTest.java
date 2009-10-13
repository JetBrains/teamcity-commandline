package jetbrains.buildServer.commandline;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.serverSide.impl.BuildTypeImpl;
import jetbrains.buildServer.vcs.VcsClientMapping;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.openapi.WebResourcesManager;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

/**
 * @author kir
 */
@Test
public class CommandLineControllerTest extends BaseControllerTestCase {
  @Override
  protected BaseController createController(final WebControllerManager manager, final WebResourcesManager resourcesManager) {
    return new CommandLineController(new MockPluginDescriptor(), manager, myFixture.getProjectManager(), myServer.getVcsManager());
  }

  public void should_fill_the_model() throws Exception {

    processRequest();
    
    assertEquals(0, ((List)getModel().get("buildTypes")).size());
    assertEquals("/buildServer1234/plugins/cmdline/", getModel().get("cmdPathPrefix"));
  }


  public void should_fill_buildTypes_with_vcs_roots() throws Exception {

    myFixture.addVcsRoot("badSvn", "");

    PathPrefixesSupport.registerVcsRootMappingSupport(new ArrayList<VcsClientMapping>(), myServer.getVcsManager() );
    final BuildTypeImpl goodBuildType = registerBuildType("supportedBuildType", "ddd", "Ant");
    myFixture.addVcsRoot("mock", "");

    processRequest();
    
    assertEquals(1, ((List)getModel().get("buildTypes")).size());
  }


  private class MockPluginDescriptor implements PluginDescriptor {
    public String getPluginName() {
      return "cmdline";
    }

    public String getPluginResourcesPath() {
      return "/plugins/cmdline/";
    }

    public String getPluginResourcesPath(@NotNull final String relativePath) {
      return getPluginResourcesPath() + "/" + relativePath;
    }
  }
}

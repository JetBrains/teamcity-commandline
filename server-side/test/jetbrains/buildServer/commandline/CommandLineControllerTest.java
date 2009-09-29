package jetbrains.buildServer.commandline;

import java.util.List;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
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
    return new CommandLineController(new MockPluginDescriptor(), manager, myFixture.getProjectManager());
  }

  public void should_fill_the_model() throws Exception {

    processRequest();
    
    assertEquals(1, ((List)getModel().get("buildTypes")).size());
    assertEquals("/buildServer1234/plugins/cmdline/", getModel().get("cmdPathPrefix"));
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

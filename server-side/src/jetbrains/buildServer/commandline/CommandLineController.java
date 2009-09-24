package jetbrains.buildServer.commandline;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NonNls;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller to handle settings for command line remote runner tool.
 * The related page is available from My Settings and Tools page, tools section.
 */
public class CommandLineController extends BaseController {

  @NonNls private static final String CONTROLLER_PATH = "/commandline.html";
  @NonNls private static final String MY_JSP = "commandlineSettings.jsp";

  private final PluginDescriptor myPluginDescriptor;
  private final WebControllerManager myWebControllerManager;

  public CommandLineController(final PluginDescriptor pluginDescriptor, final WebControllerManager webControllerManager) {
    myPluginDescriptor = pluginDescriptor;
    myWebControllerManager = webControllerManager;
  }

  @Override
  protected ModelAndView doHandle(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    if (!isPost(request)) {
      return new ModelAndView(myPluginDescriptor.getPluginResourcesPath(MY_JSP));
    }
    return null;
  }

  public void register() {
    myWebControllerManager.registerController(CONTROLLER_PATH, this);
  }
}

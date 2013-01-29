package jetbrains.buildServer.commandline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.SecurityContext;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.filters.Filter;
import jetbrains.buildServer.vcs.CheckoutRules;
import jetbrains.buildServer.vcs.VcsManager;
import jetbrains.buildServer.vcs.VcsRootInstance;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.vcs.api.services.tc.MappingGeneratorService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller to handle settings for command line remote runner tool.
 * The related page is available from My Settings and Tools page, tools section.
 */
public class CommandLineController extends BaseController {

  @NonNls private static final String CONTROLLER_PATH = "/commandline.html";
  @NonNls private static final String MY_JSP = "commandlineSettings.jsp";

  private final PluginDescriptor myPluginDescriptor;
  private final VcsManager myVcsManager;
  private final WebControllerManager myWebControllerManager;
  private final ProjectManager myProjectManager;
  private final SecurityContext mySecurityContext;

  public CommandLineController(final PluginDescriptor pluginDescriptor,
                               final WebControllerManager webControllerManager,
                               final ProjectManager projectManager, final VcsManager vcsManager,
                               final SecurityContext securityContext) {
    myPluginDescriptor = pluginDescriptor;
    myWebControllerManager = webControllerManager;
    myProjectManager = projectManager;
    myVcsManager = vcsManager;
    mySecurityContext = securityContext;
  }

  public void register() {
    myWebControllerManager.registerController(CONTROLLER_PATH, this);
  }

  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) throws Exception {
    final Map<String, Object> model = new HashMap<String, Object>();
    addBuildTypes(model);
    CommandLineSection.addPathPrefix(model, request, myPluginDescriptor);

    return new ModelAndView(myPluginDescriptor.getPluginResourcesPath(MY_JSP), model);
  }

  private void addBuildTypes(final Map<String, Object> model) {
    final List<SBuildType> buildTypes = myProjectManager.getActiveBuildTypes();
    model.put("buildTypes", collectBuildTypesWithPersonalVcsSupport(buildTypes));
  }

  private List<SBuildType> collectBuildTypesWithPersonalVcsSupport(final List<SBuildType> buildTypes) {
    return CollectionsUtil.filterCollection(buildTypes, new Filter<SBuildType>() {
      public boolean accept(@NotNull final SBuildType data) {

        return hasRootWithVcsClientMappingProvider(data) && hasRightToViewBuildTypeDetails(data);

      }
    });
  }

  private boolean hasRightToViewBuildTypeDetails(final SBuildType buildType) {
    return mySecurityContext.getAuthorityHolder().isPermissionGrantedForProject(buildType.getProjectId(), Permission.VIEW_BUILD_CONFIGURATION_SETTINGS);
  }

  private boolean hasRootWithVcsClientMappingProvider(final SBuildType data) {
    final List<VcsRootInstance> roots = data.getVcsRootInstances();

    for (VcsRootInstance entry : roots) {
      final MappingGeneratorService service =
        myVcsManager.getVcsService(entry.createVcsSettings(CheckoutRules.DEFAULT), MappingGeneratorService.class);

      if (service != null) return true;
    }
    return false;
  }

}

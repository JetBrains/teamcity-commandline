package jetbrains.buildServer.commandline;

import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.SecurityContext;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.filters.Filter;
import jetbrains.buildServer.vcs.SVcsRoot;
import jetbrains.buildServer.vcs.VcsClientMappingProvider;
import jetbrains.buildServer.vcs.VcsManager;
import jetbrains.buildServer.vcs.VcsSupportContext;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
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
    final HashMap model = new HashMap();
    addBuildTypes(model);
    CommandLineSection.addPathPrefix(model, request, myPluginDescriptor);

    return new ModelAndView(myPluginDescriptor.getPluginResourcesPath(MY_JSP), model);
  }

  private void addBuildTypes(final HashMap model) {
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
    final List<SVcsRoot> roots = data.getVcsRoots();

    for (SVcsRoot root : roots) {
      final VcsSupportContext ctx = myVcsManager.findVcsContextByName(root.getVcsName());
      if (ctx != null) {
        final VcsClientMappingProvider clientMappingProvider = ctx.getVcsExtension(VcsClientMappingProvider.class);
        if (clientMappingProvider != null) {
          return true;
        }
      }
    }
    return false;
  }

}

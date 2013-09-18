package jetbrains.buildServer.commandline;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseAjaxActionController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.VcsManager;
import jetbrains.buildServer.web.openapi.ControllerAction;
import jetbrains.vcs.api.services.tc.VcsMappingElement;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GetBuildTypeMappingAction implements ControllerAction {
  @NonNls private static final String MAPPING_FOR = "mappingFor";

  private final ProjectManager myProjectManager;
  private final VcsManager myVcsManager;
  private final BaseAjaxActionController myController;

  public GetBuildTypeMappingAction(final ProjectManager projectManager,
                                   final VcsManager vcsManager,
                                   final BaseAjaxActionController controller) {
    myProjectManager = projectManager;
    myVcsManager = vcsManager;
    myController = controller;
  }

  public boolean canProcess(@NotNull final HttpServletRequest request) {
    return StringUtil.isNotEmpty(btId(request));
  }

  public void process(@NotNull final HttpServletRequest request,
                      @NotNull final HttpServletResponse response,
                      @Nullable final Element ajaxResponse) {
    final SBuildType buildTypeById = myProjectManager.findBuildTypeByExternalId(btId(request));
    if (buildTypeById != null) {
      final MappingGenerator generator = new MappingGenerator(myVcsManager, buildTypeById);
      generator.generateVcsMapping();
      if (generator.getMappings().size() > 0 && ajaxResponse != null) {
        final Element mapping = new Element("mapping");
        ajaxResponse.addContent(mapping);
        for (VcsMappingElement mappingElement : generator.getMappings()) {
          final Element mapElement = new Element("map");
          mapElement.setAttribute("from", mappingElement.getFrom());
          mapElement.setAttribute("to", mappingElement.getTo());
          mapElement.setAttribute("comment", mappingElement.getComment());
          mapping.addContent(mapElement);
        }
      }
    }
  }

  private static String btId(final HttpServletRequest request) {
    return request.getParameter(MAPPING_FOR);
  }

  public void register() {
    myController.registerAction(this);
  }
}

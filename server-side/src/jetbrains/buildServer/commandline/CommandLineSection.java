package jetbrains.buildServer.commandline;

import jetbrains.buildServer.web.openapi.SimplePageExtension;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class CommandLineSection extends SimplePageExtension {
  private final PluginDescriptor myPluginDescriptor;

  public CommandLineSection(final PagePlaces pagePlaces, final PluginDescriptor pluginDescriptor) {
    super(pagePlaces);
    myPluginDescriptor = pluginDescriptor;
  }

  @Override
  public void fillModel(@NotNull final Map<String, Object> model, @NotNull final HttpServletRequest request) {
    super.fillModel(model, request);
    addPathPrefix(model, request, myPluginDescriptor);
  }

  static void addPathPrefix(final Map<String, Object> model, final HttpServletRequest request, final PluginDescriptor pluginDescriptor) {
    model.put("cmdPathPrefix", request.getContextPath() + pluginDescriptor.getPluginResourcesPath());
  }
}

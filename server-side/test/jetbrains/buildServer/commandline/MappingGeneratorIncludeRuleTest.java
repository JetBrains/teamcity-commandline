package jetbrains.buildServer.commandline;

import java.util.List;
import jetbrains.buildServer.vcs.CheckoutRules;
import jetbrains.buildServer.vcs.VcsClientMapping;
import jetbrains.buildServer.vcs.impl.SVcsRootImpl;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class MappingGeneratorIncludeRuleTest extends BaseMappingGeneratorTest {
  public void generate_mapping_with_target_checkout_rule() throws Exception {
    final SVcsRootImpl vcsRoot = vcsRoot();
    myBuildType.setCheckoutRules(vcsRoot, CheckoutRules.createOn("+:.=>svnrepo"));

    myPathPrefixes.add(new VcsClientMapping("UID|some/path", ""));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 1, mapping.size());

    verifyMapping(mapping.get(0), ".", "mock://UID|some/path", vcsRoot.getDescription() + "; .=>svnrepo");
  }

  public void generate_mapping_with_2_include_rules() throws Exception {
    final SVcsRootImpl vcsRoot = vcsRoot();
    myBuildType.setCheckoutRules(vcsRoot, CheckoutRules.createOn("+:.=>svnrepo\ndddPath"));

    myPathPrefixes.add(new VcsClientMapping("UID|some/path", ""));
    myPathPrefixes.add(new VcsClientMapping("UID|some/path/subpath", "subpathMapping"));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 4, mapping.size());

    verifyMapping(mapping.get(0), ".", "mock://UID|some/path", vcsRoot.getDescription() + "; dddPath=>dddPath");
    verifyMapping(mapping.get(1), "subpathMapping", "mock://UID|some/path/subpath", vcsRoot.getDescription() + "; dddPath=>dddPath");
    verifyMapping(mapping.get(2), ".", "mock://UID|some/path", vcsRoot.getDescription() + "; .=>svnrepo");
    verifyMapping(mapping.get(3), "subpathMapping", "mock://UID|some/path/subpath", vcsRoot.getDescription()+ "; .=>svnrepo");
  }

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PathPrefixesSupport.registerIncludeRuleVcsMappingSupport(myPathPrefixes, myServer.getVcsManager());
  }
}

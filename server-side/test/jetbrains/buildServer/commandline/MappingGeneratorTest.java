package jetbrains.buildServer.commandline;

import java.util.List;
import java.util.ArrayList;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.vcs.impl.SVcsRootImpl;
import jetbrains.buildServer.vcs.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

@Test
public class MappingGeneratorTest extends BaseServerTestCase {
  private List<VcsUrlInfo2TargetPath> myPathPrefixes;

  public void should_generate_empty_mapping() throws Exception {

    assertEquals(0, generateMappingForBuildType().size());

  }

  private List<MappingElement> generateMappingForBuildType() {
    final MappingGenerator generator = new MappingGenerator(myFixture.getVcsManager(), myBuildType);
    generator.generateVcsMapping();
    return generator.getLines();
  }

  public void generate_simple_mapping() throws Exception {

    final SVcsRootImpl vcsRoot = vcsRoot();
    myPathPrefixes.add(new VcsUrlInfo2TargetPath("UID|some/path", ""));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 1, mapping.size());

    final MappingElement theOnlyMappingElement = mapping.get(0);
    verifySimpleMapping(theOnlyMappingElement, vcsRoot);
  }

  private void verifySimpleMapping(final MappingElement mapping, final SVcsRootImpl vcsRoot) {
    verifyMapping(mapping, ".", "mock://UID|some/path", vcsRoot.getDescription());
  }

  private void verifyMapping(final MappingElement mapping,
                             final String localPath, final String targetLocation, final String description) {
    final MappingElement expected = new MappingElement(localPath, targetLocation, description);
    assertEquals(expected,  mapping);
  }

  public void generate_simple_mapping_2_roots() throws Exception {

    final SVcsRootImpl vcsRoot1 = vcsRoot();
    final SVcsRootImpl vcsRoot2 = vcsRoot();
    vcsRoot1.addProperty("url", "url path 1");
    vcsRoot2.addProperty("url", "url path 2");
    myPathPrefixes.add(new VcsUrlInfo2TargetPath("UID|some/path", ""));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 2, mapping.size());

    verifySimpleMapping(mapping.get(0), vcsRoot1);
    verifySimpleMapping(mapping.get(1), vcsRoot2);
  }

  public void generate_mapping_with_several_paths() throws Exception {

    final SVcsRootImpl vcsRoot = vcsRoot();

    myPathPrefixes.add(new VcsUrlInfo2TargetPath("UID|some/path", ""));
    myPathPrefixes.add(new VcsUrlInfo2TargetPath("UID|some/path/subpath", "path3"));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 2, mapping.size());

    verifyMapping(mapping.get(0), ".", "mock://UID|some/path", vcsRoot.getDescription());
    verifyMapping(mapping.get(1), "path3", "mock://UID|some/path/subpath", vcsRoot.getDescription());
  }

  public void generate_mapping_with_target_checkout_rule() throws Exception {

    final SVcsRootImpl vcsRoot = vcsRoot();
    myBuildType.setCheckoutRules(vcsRoot, CheckoutRules.createOn("+:.=>svnrepo"));

    myPathPrefixes.add(new VcsUrlInfo2TargetPath("UID|some/path", ""));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 1, mapping.size());

    verifyMapping(mapping.get(0), ".", "mock://UID|some/path", vcsRoot.getDescription() + "; .=>svnrepo");
  }

  public void generate_mapping_with_2_include_rules() throws Exception {

    final SVcsRootImpl vcsRoot = vcsRoot();
    myBuildType.setCheckoutRules(vcsRoot, CheckoutRules.createOn("+:.=>svnrepo\ndddPath"));

    myPathPrefixes.add(new VcsUrlInfo2TargetPath("UID|some/path", ""));
    myPathPrefixes.add(new VcsUrlInfo2TargetPath("UID|some/path/subpath", "subpathMapping"));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 4, mapping.size());

    verifyMapping(mapping.get(0), ".", "mock://UID|some/path", vcsRoot.getDescription() + "; dddPath=>dddPath");
    verifyMapping(mapping.get(1), "subpathMapping", "mock://UID|some/path/subpath", vcsRoot.getDescription() + "; dddPath=>dddPath");
    verifyMapping(mapping.get(2), ".", "mock://UID|some/path", vcsRoot.getDescription() + "; .=>svnrepo");
    verifyMapping(mapping.get(3), "subpathMapping", "mock://UID|some/path/subpath", vcsRoot.getDescription()+ "; .=>svnrepo");
  }

  // TODO: kir - error reporting


  private SVcsRootImpl vcsRoot() {
    return myFixture.addVcsRoot("mock", "");
  }

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();    //To change body of overridden methods use File | Settings | File Templates.
    myPathPrefixes = new ArrayList<VcsUrlInfo2TargetPath>();
    PathPrefixesSupport.register(myPathPrefixes, myServer.getVcsManager());
  }

}

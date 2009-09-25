package jetbrains.buildServer.commandline;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.impl.MockVcsSupport;
import jetbrains.buildServer.serverSide.impl.MockPersonalSupport;
import jetbrains.buildServer.vcs.impl.SVcsRootImpl;
import jetbrains.buildServer.vcs.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.jetbrains.annotations.NotNull;

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
    verifyMapping(mapping, vcsRoot, ".", "UID|some/path");
  }

  private void verifyMapping(final MappingElement mapping,
                             final SVcsRootImpl vcsRoot,
                             final String localPath, final String targetLocation) {
    final MappingElement expected = new MappingElement(localPath, targetLocation, vcsRoot.getDescription());
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

    verifyMapping(mapping.get(0), vcsRoot, ".", "UID|some/path");
    verifyMapping(mapping.get(1), vcsRoot, "path3", "UID|some/path/subpath");
  }

  public void generate_mapping_with_target_checkout_rule() throws Exception {

    final SVcsRootImpl vcsRoot = vcsRoot();
    myBuildType.setCheckoutRules(vcsRoot, CheckoutRules.createOn("+:.=>svnrepo"));

    myPathPrefixes.add(new VcsUrlInfo2TargetPath("UID|some/path", ""));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 1, mapping.size());

    verifyMapping(mapping.get(0), vcsRoot, "svnrepo", "UID|some/path");
  }

  public void generate_mapping_with_2_include_rules() throws Exception {

    final SVcsRootImpl vcsRoot = vcsRoot();
    myBuildType.setCheckoutRules(vcsRoot, CheckoutRules.createOn("+:.=>svnrepo\ndddPath"));

    myPathPrefixes.add(new VcsUrlInfo2TargetPath("UID|some/path", ""));
    myPathPrefixes.add(new VcsUrlInfo2TargetPath("UID|some/path/subpath", "subpathMapping"));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 4, mapping.size());

    verifyMapping(mapping.get(0), vcsRoot, "dddPath", "UID|some/path");
    verifyMapping(mapping.get(1), vcsRoot, "dddPath/subpathMapping", "UID|some/path/subpath");
    verifyMapping(mapping.get(2), vcsRoot, "svnrepo", "UID|some/path");
    verifyMapping(mapping.get(3), vcsRoot, "svnrepo/subpathMapping", "UID|some/path/subpath");
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
    registerMockVcsSupport();
  }

  private void registerMockVcsSupport() {
    myFixture.getVcsManager().registerVcsSupport(new MockVcsSupport("mock") {
      @Override
      public VcsPersonalSupport getPersonalSupport() {
        return new MockPersonalSupport() {
          @Override
          public Collection<VcsUrlInfo2TargetPath> getPossiblePathPrefixes(@NotNull final VcsRoot vcsRoot,
                                                                           @NotNull final IncludeRule includeRule)
            throws VcsException {
            return myPathPrefixes;
          }
        };
      }
    });
  }

}

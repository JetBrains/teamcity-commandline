package jetbrains.buildServer.commandline;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.vcs.VcsClientMapping;
import jetbrains.buildServer.vcs.impl.SVcsRootImpl;
import org.testng.annotations.*;

/**
 * @author kir
 */
@Test
public abstract class BaseMappingGeneratorTest extends BaseServerTestCase {
  protected List<VcsClientMapping> myPathPrefixes;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();   
    myPathPrefixes = new ArrayList<VcsClientMapping>();
  }

  public void should_generate_empty_mapping() throws Exception {
    assertEquals(0, generateMappingForBuildType().size());
  }

  protected List<MappingElement> generateMappingForBuildType() {
    final MappingGenerator generator = new MappingGenerator(myFixture.getVcsManager(), myBuildType);
    generator.generateVcsMapping();
    return generator.getMappings();
  }

  public void generate_simple_mapping() throws Exception {

    final SVcsRootImpl vcsRoot = vcsRoot();
    myPathPrefixes.add(new VcsClientMapping("UID|some/path", ""));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 1, mapping.size());

    final MappingElement theOnlyMappingElement = mapping.get(0);
    verifySimpleMapping(theOnlyMappingElement, vcsRoot);
  }

  public void generate_simple_mapping_2_roots() throws Exception {

    final SVcsRootImpl vcsRoot1 = vcsRoot();
    final SVcsRootImpl vcsRoot2 = vcsRoot();
    myPathPrefixes.add(new VcsClientMapping("UID|some/path", ""));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 2, mapping.size());

    verifySimpleMapping(mapping.get(0), vcsRoot1);
    verifySimpleMapping(mapping.get(1), vcsRoot2);
  }

  public void generate_mapping_with_several_paths() throws Exception {

    final SVcsRootImpl vcsRoot = vcsRoot();

    myPathPrefixes.add(new VcsClientMapping("UID|some/path", ""));
    myPathPrefixes.add(new VcsClientMapping("UID|some/path/subpath", "path3"));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 2, mapping.size());

    verifyMapping(mapping.get(0), "path3", "mock://UID|some/path/subpath", vcsRoot.getDescription());
    verifyMapping(mapping.get(1), ".", "mock://UID|some/path", vcsRoot.getDescription());
  }

  protected void verifySimpleMapping(final MappingElement actual, final SVcsRootImpl vcsRoot) {
    verifyMapping(actual, ".", "mock://UID|some/path", vcsRoot.getDescription());
  }

  protected void verifyMapping(final MappingElement actual,
                             final String localPath, final String targetLocation, final String description) {
    final MappingElement expected = new MappingElement(localPath, targetLocation, description);
    assertEquals(expected, actual);
  }

  protected SVcsRootImpl vcsRoot() {
    return myFixture.addVcsRoot("mock", "");
  }
}

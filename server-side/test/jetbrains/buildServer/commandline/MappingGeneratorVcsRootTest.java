package jetbrains.buildServer.commandline;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.vcs.CheckoutRules;
import jetbrains.buildServer.vcs.VcsClientMapping;
import jetbrains.buildServer.vcs.impl.SVcsRootImpl;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class MappingGeneratorVcsRootTest extends BaseMappingGeneratorTest {
  public void generate_mapping_with_include_rule_1() throws Exception {
    doTest(".=>svnrepo", new String[] {
      "svnrepo/aaa", "mock://AAAPATH",
      "svnrepo", "mock://PATH",
    });
  }

  public void generate_mapping_with_include_rule_2() throws Exception {
    doTest("svnrepo=>.", new String[] {
      ".", "mock://PATH/svnrepo"
    });
  }

  public void generate_mapping_with_include_rule_3() throws Exception {
    doTest("aaa=>.", new String[] {
      ".", "mock://AAAPATH"
    });
  }

  public void generate_mapping_with_include_rule_4() throws Exception {
    doTest("aa=>.", new String[] {
      ".", "mock://PATH/aa"
    });
  }

  public void generate_mapping_with_include_rule_5() throws Exception {
    doTest("a=>b", new String[] {
      "b", "mock://PATH/a"
    });
  }

  public void generate_mapping_with_include_rule_6() throws Exception {
    doTest("aaa=>bbb", new String[] {
      "bbb", "mock://AAAPATH"
    });
  }

  private void doTest(final String checkoutRule, final String[] expectations) {
    myPathPrefixes.add(new VcsClientMapping("PATH", ""));
    myPathPrefixes.add(new VcsClientMapping("AAAPATH", "aaa"));

    final SVcsRootImpl vcsRoot = vcsRoot();
    myBuildType.setCheckoutRules(vcsRoot, CheckoutRules.createOn(checkoutRule));

    final List<MappingElement> mapping = generateMappingForBuildType();

    assertEquals(expectations.length / 2, mapping.size());

    verifyMapping(mapping.get(0), expectations[0], expectations[1], vcsRoot.getDescription() + "; " + checkoutRule);
    if (expectations.length > 2) {
      verifyMapping(mapping.get(1), expectations[2], expectations[3], vcsRoot.getDescription() + "; " + checkoutRule);
    }
  }

  public void generate_mapping_with_2_include_rules() throws Exception {
    final SVcsRootImpl vcsRoot = vcsRoot();
    myBuildType.setCheckoutRules(vcsRoot, CheckoutRules.createOn(".=>svnrepo\na=>b"));

    myPathPrefixes.add(new VcsClientMapping("UID1|path", ""));

    final List<MappingElement> mapping = generateMappingForBuildType();
    assertEquals(mapping.toString(), 2, mapping.size());

    verifyMapping(mapping.get(0), "svnrepo", "mock://UID1|path", vcsRoot.getDescription() + "; .=>svnrepo");
    verifyMapping(mapping.get(1), "b", "mock://UID1|path/a", vcsRoot.getDescription() + "; a=>b");
  }

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();    //To change body of overridden methods use File | Settings | File Templates.
    myPathPrefixes = new ArrayList<VcsClientMapping>();
    PathPrefixesSupport.registerVcsRootMappingSupport(myPathPrefixes, myServer.getVcsManager());
  }
}

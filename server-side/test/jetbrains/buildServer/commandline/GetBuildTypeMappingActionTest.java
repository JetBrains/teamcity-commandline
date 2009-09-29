package jetbrains.buildServer.commandline;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.BaseWebTestCase;
import jetbrains.buildServer.controllers.MockRequest;
import jetbrains.buildServer.controllers.MockResponse;
import jetbrains.buildServer.util.XmlUtil;
import jetbrains.buildServer.vcs.VcsUrlInfo2TargetPath;
import org.jdom.Element;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


@Test
public class GetBuildTypeMappingActionTest extends BaseWebTestCase {
  private List<VcsUrlInfo2TargetPath> myPathPrefixes;
  private GetBuildTypeMappingAction myAction;


  public void should_process_empty_request() throws Exception {

    assertFalse(myAction.canProcess(new MockRequest()));
    assertTrue(myAction.canProcess(new MockRequest("mappingFor", "bt11")));

    final Element response = runActionForBuildType();

    assertEquals("<response />", XmlUtil.to_s(response));
  }

  private Element runActionForBuildType() {
    final Element response = new Element("response");
    myAction.process(new MockRequest("mappingFor", myBuildType.getId()), new MockResponse(), response);
    return response;
  }

  public void should_process_request_with_data() throws Exception {
    myFixture.addVcsRoot("mock", "");
    myPathPrefixes.add(new VcsUrlInfo2TargetPath("perforce://rusps-app01:1666:////depot/src/", ""));

    final Element response = runActionForBuildType();

    assertEquals(XmlUtil.to_s(XmlUtil.from_s(
      "<response>" +
      "  <mapping>" +
      "    <map from=\".\" to=\"perforce://rusps-app01:1666:////depot/src\" comment=\"mock\" />" +
      "  </mapping>" +
      "</response>"))
    , XmlUtil.to_s(response));

  }

  @BeforeMethod
  protected void setUp() throws Exception {
    super.setUp();
    myAction = new GetBuildTypeMappingAction(myServer.getProjectManager(), myServer.getVcsManager(), null);
    myPathPrefixes = new ArrayList<VcsUrlInfo2TargetPath>();
    PathPrefixesSupport.register(myPathPrefixes, myServer.getVcsManager());
  }
}

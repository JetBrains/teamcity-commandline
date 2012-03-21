<%@ page import="jetbrains.buildServer.web.util.WebUtil" %>
<%@ page import="jetbrains.buildServer.web.util.SessionUser" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" session="true" errorPage="/runtimeError.jsp"
  %><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
  %><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
  %><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"
  %><%@ taglib prefix="bs" tagdir="/WEB-INF/tags"
  %><%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout"
  %><%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms"
  %><%@ taglib prefix="authz" tagdir="/WEB-INF/tags/authz"
  %><%@ taglib prefix="afn" uri="/WEB-INF/functions/authz"
  %><%@ taglib prefix="graph" tagdir="/WEB-INF/tags/graph" %>

<c:set var="pageTitle" value="Command Line Tool Settings" scope="request"/>
<c:set var="loginLine" >java -jar c:\tcc.jar login --host <%= WebUtil.getRootUrl(request)%> --user <%= SessionUser.getUser(request).getUsername()%></c:set>
<c:set var="remoteRunLine" >java -jar c:\tcc.jar run --host <%= WebUtil.getRootUrl(request)%> -m "remote run message" -c <span id="btId">btXXX</span> &lt;list of modified files&gt;</c:set>
<bs:page>

<jsp:attribute name="head_include">

  <bs:linkCSS>
    ${cmdPathPrefix}commandLine.css
  </bs:linkCSS>

  <bs:linkScript>
    ${cmdPathPrefix}commandLine.js
  </bs:linkScript>

  <!-- ===== JS files, provided by plugins: ==== -->

  <script type="text/javascript">
    <!--

    BS.Navigation.items = [
      { title: "My Settings & Tools",
        url: "profile.html",
        selected:false
      },
      { title: "${pageTitle}",
        url: "",
        selected:true
      }
    ];

    <jsp:useBean id="buildTypes" type="java.util.List<jetbrains.buildServer.serverSide.SBuildType>" scope="request"/>
    BS.BuildTypes = [
      <c:forEach items="${buildTypes}" var="buildType">
      {id: "${buildType.id}", fullName: '<bs:escapeForJs text="${buildType.fullName}"/>'},</c:forEach>
      {}
    ];
    //-->
  </script>
</jsp:attribute>

<jsp:attribute name="body_include">

  <p>
    On this page you can generate a configuration file for a tool which allows to <strong>run personal builds</strong> from <strong>command line</strong>.
  </p>
  <p>
    To operate, this tool requires a <strong>configuration file</strong> which maps local paths in your project to VCS settings in Teamcity.
  </p>

  <h3>Create configuration file</h3>

  <div>
    Add configuration for
    <select name="buildConfigurationSelector" id="buildConfigurationSelector">
      <option>-- Select a build configuration --</option>
    </select>
    &nbsp;&nbsp;
    <button id="addMapping" disabled="true" title="Add VCS mapping for selected build configuration">Add</button>
    <forms:saving id="updateIndicator" style="float: none;" savingTitle="Adding VCS mapping for selected configuration, please wait"/>
  </div>

  <div class="hiddenBlock">

    The changes made in the table are <strong>automatically</strong> reflected in the resulting configuration file contents, below.

    <table id="mappingTable" class="dark">
      <tr>
        <th class="fromInput">Local Path</th>
        <th class="toInput">VCS Path</th>
        <th>VCS Source Details</th>
        <th class="remove">Remove</th>
      </tr>
    </table>

    <div class="resultsPreview">
      <%--<a href="#" onclick="BS.CommandLine.updatePreview(); return false;" style="float: right;">Update from table</a>--%>
      <h3>Resulting configuration file contents:</h3>
      <textarea rows="7" cols="80" id="resultsConfig" readonly="true" onclick="$(this).activate(); return false;"></textarea>
    </div>


    <h3 style="margin-top: 3.2em;">Each line in the configuration file contains two parts:</h3>
    <ol>
      <li><strong>Local path</strong> in your project, which can be either absolute path on your hard drive or it can be relative to the project root.</li>
      <li><strong>VCS path</strong>. This path points to a location under VCS root and is formatted according to TeamCity formatting rules.
        It usually comprises identifier of the VCS repository and a path within this repository.</li>
    </ol>
      
    <c:set var="mappingsFile" value=".teamcity-mappings.properties"/>
    <h3 style="clear: both;">Steps to run personal build from command line:</h3>
    <ol>
      <li>Install <a href="http://java.com">Java JRE 1.5.+</a> on your machine, make sure <code>java</code> is available in the command line</li>
      <li><a href="${cmdPathPrefix}tcc.jar">Download the tool (tcc.jar)</a> to your hard drive, for instance to <code>c:\tcc.jar</code></li>
      <li>Copy contents of the configuration file (above) to <code>${mappingsFile}</code> in your root project directory (you may decide to add this file under the version control)</li>
      <li>Login to TeamCity with command line runner tool:<pre>${loginLine}</pre> </li>
      <li>Run remote build for contents of a directory with selected build configuration: <pre>${remoteRunLine}</pre>
      <br/>To learn more about the tool options, please see <a href="http://svn.jetbrains.org/teamcity/plugins/commandline/trunk/command.line/HOWTO">documentation</a></li>
    </ol>

  </div>


</jsp:attribute>

</bs:page>

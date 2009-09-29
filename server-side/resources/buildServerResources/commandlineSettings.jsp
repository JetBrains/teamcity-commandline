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

<c:set var="title" value="Command Line Tool Settings" scope="request"/>
<bs:page>

<jsp:attribute name="head_include">

  <style type="text/css">
    @import "${cmdPathPrefix}commandLine.css";
  </style>
  <script type="text/javascript" src="${cmdPathPrefix}commandLine.js"></script>


  <!-- ===== JS files, provided by plugins: ==== -->

  <script type="text/javascript">
    <!--

    BS.Navigation.items = [
      { title: "My Settings & Tools",
        url: "profile.html",
        selected:false
      },
      { title: "${title}",
        url: "",
        selected:true
      }
      ];

    <jsp:useBean id="buildTypes" type="java.util.List<jetbrains.buildServer.serverSide.SBuildType>" scope="request"/>
    BS.BuildTypes = [
      <c:forEach items="${buildTypes}" var="buildType">
      {"id": "${buildType.id}", fullName: '<bs:escapeForJs text="${buildType.fullName}"/>'},</c:forEach>
      {}
    ];
    //-->
  </script>
</jsp:attribute>

<jsp:attribute name="body_include">

  <p>
      On this page you can generate a configuration file for a tool which allows to <strong>run personal builds</strong> from <strong>command line</strong>.
      <a href="${cmdPathPrefix}tcc.jar">Download the tool</a>
  </p>
  <p>
      To operate, this tool requires a <strong>configuration file</strong> which maps local paths in your project to VCS settings in Teamcity.
  </p>
  <p>
      Each line in the configuration file contains two parts:
  </p>
  <ol>
    <li><strong>Local path</strong> in your project, which can be either absolute path on your hard drive or it can be relative to the project root.</li>
    <li><strong>VCS path</strong>. This path points to a location under VCS root and is formatted according to TeamCity formatting rules.
      It usually comprises identifier of the VCS repository and path within this repository.</li>
  </ol>

  <div>
    Insert mapping from:
    <select name="buildConfigurationSelector" id="buildConfigurationSelector">
      <option>-- Select a build configuration --</option>
    </select>

    <button id="addMapping" disabled="true" title="Add VCS mapping for selected build configuration">Add mapping</button>
    <forms:saving id="updateIndicator" style="float: none;" savingTitle="Adding VCS mapping for selected configuration, please wait"/>
  </div>

  <div style="display: none;">

    <a href="#" onclick="BS.CommandLine.removeAll(); return false;" class="red actionLink" style="float:right; margin-bottom: .2em;">Remove all</a> 
    <table id="mappingTable" class="dark">
      <tr>
        <th class="fromInput">Local Path</th>
        <th class="toInput">VCS Path</th>
        <th>VCS description</th>
        <th class="remove">Remove</th>
      </tr>
    </table>

    <div class="resultsPreview">
      <a href="#" onclick="BS.CommandLine.updatePreview(); return false;" style="float: right;">Update from table</a>
      <strong>Resulting configuration file contents:</strong><br/>
      <textarea rows="10" cols="80" id="resultsConfig"></textarea>
    </div>

  </div>


</jsp:attribute>

</bs:page>

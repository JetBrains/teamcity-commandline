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

  <bs:linkCSS>
  </bs:linkCSS>
  <bs:linkScript>
  </bs:linkScript>


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
      {"id": "${buildType.id}", fullName: '<bs:escapeForJs text="${buildType.fullName}"/>'},
      </c:forEach>
      {}
    ];

    function fillBuildTypesList() {
      var select = $('buildConfigurationSelector');
      while(select.length > 1) {
        select.remove(1);
      }

      for(var i = 0; i < BS.BuildTypes.length; i ++) {
        var buildType = BS.BuildTypes[i];
        if (buildType.id) {
          var option = document.createElement("option");
          option.value = buildType.id;
          option.text  = buildType.fullName;
          select.add(option, null);
        }
      }
    }

    function installControlHandlers() {
      var select = $('buildConfigurationSelector');
      select.observe("change", function() {
        $('addMapping').disabled = select.selectedIndex == 0;

        //var el = select.options[select.selectedIndex];

      });

      $('addMapping').observe("click", function() {
      });
    }

    document.observe("dom:loaded", function() {
      fillBuildTypesList();
      installControlHandlers();
    });


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
  <p>
      Each line in the configuration file contains two parts:
  </p>
  <ol>
    <li><strong>Local path</strong> in your project, which can be either absolute path on your hard drive or it can be relative to the project root.</li>
    <li>Corresponding <strong>path in VCS</strong>. This path is formatted according to TeamCity formatting rules, and usually comprises identifier of the VCS repository and path within this repository.</li>
  </ol>

  <div>
    Insert mapping from:
    <select name="buildConfigurationSelector" id="buildConfigurationSelector">
      <option>-- Select a build configuration --</option>
    </select>

    <button id="addMapping" disabled="true">Add mapping</button>
  </div>


  <p>Resulting configuration file contents:</p>
  <textarea rows="10" cols="80" id="results"></textarea>

</jsp:attribute>

</bs:page>

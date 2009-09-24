<%@ include file="../../include.jsp" %>
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

    //-->
  </script>
</jsp:attribute>

<jsp:attribute name="body_include">

  Some stuff

</jsp:attribute>

</bs:page>

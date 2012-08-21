BS.CommandLine = {

  ROWS: [
    {css: "fromInput", template: new Template("<input type='text' name='from' value='#{from}'/>" +
                                              "<div class='errorDup' style='display:none;'>Non-unique path, ignored</div>")},
    {css: "toInput", template: new Template("<input type='text' name='to' value='#{to}'/>")},
    {css: "comment", template: new Template("#{comment}")},
    {css: "remove", template: new Template("<a title='Remove this mapping' onclick='BS.CommandLine.removeRow(this.parentNode.parentNode); BS.CommandLine.updatePreview(); return false;' href='#' class='actionLink red'>Remove</a>")}
  ],

  selectElement: function() {
    return $('buildConfigurationSelector');
  },

  fillBuildTypesList: function() {
    this.removeAllButFirst();
    this.addOptionsFromModel();
  },

  removeAllButFirst: function() {
    var selectElement = this.selectElement();
    while (selectElement.length > 1) {
      selectElement.remove(1);
    }
  },

  addOptionsFromModel: function() {
    var selectElement = this.selectElement();
    for (var i = 0; i < BS.BuildTypes.length; i ++) {
      var buildType = BS.BuildTypes[i];

      if (buildType.id) {
        var option = document.createElement("option");
        option.value = buildType.id;
        option.text = "[" + buildType.id + "] " + buildType.fullName;
        try {
          selectElement.add(option, null);
        }
        catch(e) {
          selectElement.add(option);
        }
      }
    }
  },

  installControlHandlers: function() {
    var select = this.selectElement();
    select.on("change", function() {
      $('addMapping').disabled = this.selectedIndex == 0;
    });

    $('addMapping').on("click", function() {
      var buildTypeId = this.buildTypeId();
      if (buildTypeId) {
        this.addMappingFor(buildTypeId);
        if ($('btId')) {
          $('btId').innerHTML = buildTypeId;
        }
      }
    }.bind(this));
  },

  buildTypeId: function() {
    var selectElement = this.selectElement();
    if (selectElement.selectedIndex > 0) {
      return selectElement.options[selectElement.selectedIndex].value;
    }
    return null;
  },

  addMappingFor: function(buildTypeId) {
    $('updateIndicator').show();
    BS.ajaxRequest("ajax.html?mappingFor=" + buildTypeId, {
      onSuccess: function(response) {
        var root = BS.Util.documentRoot(response);
        if (!root) return;

        /*
      "<response>" +
      "  <mapping>" +
      "    <map from=\".\" to=\"perforce://rusps-app01:1666:////depot/src\" comment=\"mock\" />" +
      "  </mapping>" +
      "</response>"))
        */
        var mapElements = root.getElementsByTagName("map");
        if (mapElements.length > 0) {
          this.showMapping();
          for(var i = 0; i < mapElements.length; i ++) {
            this.addMappingRowFromXml(mapElements[i]);
          }
          this.updatePreview();
        }

      }.bind(this),
      onComplete: function() {
        $('updateIndicator').hide();
      }
    });
  },

  showMapping: function() {
    $('mappingTable').parentNode.style.display = 'block';
  },

  hideMapping: function() {
    $('mappingTable').parentNode.style.display = 'none';
  },

  addMappingRowFromXml: function(rowElement) {
    this.addMappingRow(
        rowElement.getAttribute("from"),
        rowElement.getAttribute("to"),
        rowElement.getAttribute("comment")
        );
  },

  addMappingRow: function(from, to, comment) {
    var tbody = $('mappingTable').getElementsByTagName('TBODY')[0];
    var tr = document.createElement("TR");
    this.ROWS.each(function(row) {
      var td = document.createElement("TD");
      td.innerHTML = row.template.evaluate({from: from, to: to, comment: comment});
      td.className = row.css;
      tr.appendChild(td);
    });
    tbody.appendChild(tr);

    $('mappingTable').select("input").each(function(input_element) {
      input_element.onblur = BS.CommandLine.updatePreview;
      input_element.onkeypress = BS.CommandLine.updatePreviewDelayed;
    });
  },

  updatePreview: function() {
    var left2Tr = {};
    var results = jQuery('#mappingTable').find('tr');
    $('resultsConfig').value = '';
    results.each(function() {
      var trElement = $(this);
      var inputs = trElement.getElementsByTagName('input');
      if (!inputs || inputs.length != 2) return;

      var fromInput = inputs[0],
          toInput = inputs[1];

      var from = fromInput.value;
      var to = toInput.value;

      if (left2Tr[from]) {
        trElement.down("div.errorDup").show();
        var duplicated = $(left2Tr[from].getElementsByTagName('input')[0]);
        duplicated.addClassName("duplicated");
        duplicated.title = "This path is duplicated below";

        fromInput.addClassName("duplicate");
      }
      else {
        left2Tr[from] = trElement;
        fromInput.removeClassName("duplicated");
        fromInput.removeClassName("duplicate");
        fromInput.title = "";
        trElement.down("div.errorDup").hide();

        $('resultsConfig').value += from + "=" + to + "\r\n";
      }
    });

    if (results.length == 1) {
      this.hideMapping();
    }
  },

  updatePreviewDelayed: function() {
    setTimeout(BS.CommandLine.updatePreview, 20);
  },

  removeRow: function(tr_element) {
    $(tr_element).select("input").each(function(input_element) {
      input_element.onblur = null;
      input_element.onkeypress = null;
    });
    $(tr_element).remove();
  }

};


document.observe("dom:loaded", function() {
  BS.CommandLine.fillBuildTypesList();
  BS.CommandLine.installControlHandlers();
});


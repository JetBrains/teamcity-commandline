BS.CommandLine = {

  ROW_TEMPLATE: new Template("<tr><td class='fromInput'><input type='text' name='from' value='#{from}'/></td>" +
                            "<td class='toInput'><input type='text' name='to' value='#{to}'/></td>" +
                            "<td class='comment'>#{comment}</td>" +
                            "<td class='remove'><a title='Remove this mapping' onclick='$(this.parentNode.parentNode).remove(); BS.CommandLine.updatePreview(); return false;' href='#' class='actionLink red'>Remove</a></td></tr>"),

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
        option.text = buildType.fullName;
        selectElement.add(option, null);
      }
    }
  },

  installControlHandlers: function() {
    var select = this.selectElement();
    select.observe("change", function() {
      $('addMapping').disabled = select.selectedIndex == 0;
    });

    $('addMapping').observe("click", function() {
      var buildTypeId = this.buildTypeId();
      if (buildTypeId) {
        this.addMappingFor(buildTypeId);
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
    $('mappingTable').innerHTML += this.ROW_TEMPLATE.evaluate({from: from, to: to, comment: comment});
  },

  updatePreview: function() {
    $('resultsConfig').value = '';
    $$('#mappingTable tr').each(function(trElement) {
      var inputs = trElement.getElementsByTagName('input');
      if (!inputs || inputs.length != 2) return;
      var from = inputs[0].value; 
      var to = inputs[1].value;
      $('resultsConfig').value += from + "=" + to + "\r\n";
    }, this);  
  },

  removeAll: function() {
    $$('#mappingTable tr').each(function(trElement) {
      var inputs = trElement.getElementsByTagName('input');
      if (!inputs || inputs.length != 2) return;
      trElement.remove();
    }, this);
    this.updatePreview();
    this.hideMapping();
  }

};


document.observe("dom:loaded", function() {
  BS.CommandLine.fillBuildTypesList();
  BS.CommandLine.installControlHandlers();
});


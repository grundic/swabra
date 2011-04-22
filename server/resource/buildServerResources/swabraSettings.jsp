<%--
  ~ Copyright 2000-2011 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="handlePresent" scope="request" type="java.lang.Boolean"/>
<jsp:useBean id="requestUrl" type="java.lang.String" scope="request"/>
<jsp:useBean id="buildTypeId" type="java.lang.String" scope="request"/>

<c:set var="enabledSelected" value="${propertiesBean.properties['swabra.enabled']}"/>
<c:set var="processesSelected" value="${propertiesBean.properties['swabra.processes']}"/>
<c:set var="displaySwabraSettings" value="${empty propertiesBean.properties['swabra.enabled'] ? false : true}"/>

<tr>
  <td colspan="2">
    <em>Cleans checkout directory by deleting files created during the build.</em><bs:help file="Build+Files+Cleaner+(Swabra)"/>
  </td>
</tr>
<tr class="noBorder">
  <th>Files cleanup:</th>
  <td>
    <props:selectProperty name="swabra.enabled" onchange="BS.Swabra.onEnabledChange()">
      <props:option value=""
                    selected="${empty enabledSelected}">&lt;Do not clean up&gt;</props:option>
      <props:option value="swabra.before.build"
                    selected="${not empty enabledSelected && enabledSelected != 'swabra.after.build'}">Before next build start</props:option>
      <props:option value="swabra.after.build"
                    selected="${enabledSelected == 'swabra.after.build'}">After build finish</props:option>
    </props:selectProperty>
  </td>
</tr>

<tr class="noBorder" id="swabra.clashing.warning.container" style="display:none;">
    <td colspan="2">
        <div class="attentionComment" id="clashing.warning">
        </div>
    </td>
</tr>

<tr class="noBorder" id="swabra.strict.container"
    style="${displaySwabraSettings ? '' : 'display: none;'}">
  <th>Clean checkout:</th>
  <td>
    <props:checkboxProperty name="swabra.strict" onclick="BS.Swabra.onStrictChange()"/>
    <label for="swabra.strict">Force clean checkout if cannot restore clean directory state</label>
  </td>
</tr>

<tr class="noBorder" id="swabra.rules.container"
    style="${displaySwabraSettings ? '' : 'display: none;'}">
  <th>Paths to monitor: <bs:help file="Adding+Swabra+as+a+Build+Feature" anchor="ConfiguringSwabraOptions"/></th>
  <td>
    <props:multilineProperty name="swabra.rules" rows="5" cols="40" linkTitle="Edit paths"/>
    <div class="smallNote" style="margin-left: 0;">
      Newline or comma delimited set of <strong>+|-:relative_path</strong> rules.<br/>
      By default all paths are included. Rules on any path should come in order from more abstract to more concrete,
      e.g. use <strong>-:**/dir/**</strong> to exclude all <strong>dir</strong> folders and their content,
      or <strong>-:some/dir, +:some/dir/inner</strong> to exclude <strong>some/dir</strong> folder and all it's content
      except <strong>inner</strong> subfolder and it's content.<br/>
    </div>
</tr>

<tr class="noBorder">
  <th>Locking processes:</th>
  <td>
    <props:selectProperty name="swabra.processes" onchange="BS.Swabra.onProcessesChange()">
      <props:option value=""
                    selected="${empty processesSelected}">&lt;Do not detect&gt;</props:option>
      <props:option value="report"
                    selected="${processesSelected == 'report'}">Report</props:option>
      <props:option value="kill"
                    selected="${processesSelected == 'kill'}">Kill</props:option>
    </props:selectProperty>

    <span class="smallNote" id="swabra.processes.note" style="${empty processesSelected ? 'display: none;' : ''}">
      Before the end of the build inspect the checkout directory for processes locking files in this directory.
    </span>
    <span class="smallNote" id="swabra.processes.report.note" style="${processesSelected == 'report' ? '' : 'display: none;'}">
      Report about such processes in the build log.
      <br/>
    </span>
    <span class="smallNote" id="swabra.processes.kill.note" style="${processesSelected == 'kill' ? '' : 'display: none;'}">
      Report about such processes in the build log and kill them.
      <br/>
    </span>
  </td>
</tr>

<tr class="noBorder" id="swabra.verbose.container"
    style="${displaySwabraSettings ? '' : 'display: none;'}">
  <th><label for="swabra.verbose">Verbose output:</label></th>
  <td>
    <props:checkboxProperty name="swabra.verbose"/>
  </td>
</tr>

<c:choose>
  <c:when test="${not handlePresent}">
    <c:set var="actionName" value="Install"/>
  </c:when>
  <c:otherwise>
    <c:set var="actionName" value="Update"/>
  </c:otherwise>
</c:choose>

<tr class="noBorder" style="${empty processesSelected ? 'display: none;' : ''}" id="swabra.download.handle.container">
  <td colspan="2">
    <div class="${not handlePresent ? 'attentionComment' : ''}">
      <c:if test="${not handlePresent}">
        Note that for locking processes detection handle.exe tool is required on agents.<br/>
      </c:if>
      <c:url var="handleDownloader" value="/admin/handle.html"/>
      <a href="${handleDownloader}" target="_blank" showdiscardchangesmessage="false">${actionName} SysInternals handle.exe</a>
    </div>
  </td>
</tr>

<script type="text/javascript">
  BS.Swabra = {
    updateClashing: function(enabled, strict, rules) {
      BS.ajaxRequest(base_uri + '${requestUrl}', {
        parameters: 'id=${buildTypeId}' + '&updateClashing=true' +
                    (enabled.length > 0 ? '&swabra.enabled=' + encodeURIComponent(enabled): '') +
                    '&swabra.strict=' + (strict ? strict : 'false') +
                    (rules.length > 0 ? '&swabra.rules=' + encodeURIComponent(rules) : ''),
        method : 'get',
        onComplete: function(transport) {
          var xml = transport.responseXML;
          var buildTypes = xml.firstChild.getElementsByTagName("buildType");
          if (buildTypes && buildTypes.length > 0) {
            var message = 'Build configuration' + (buildTypes.length > 1 ? 's' : '') + ' <b>';
            for (var i = 0; i < buildTypes.length; ++i) {
              message += buildTypes[i].firstChild.nodeValue + (i == buildTypes.length - 1 ? '' : ', ');
            }
            message += '</b> ha' + (buildTypes.length > 1 ? 've' : 's') +
                       ' the same checkout directory, but different Swabra cleanup settings.' +
                       ' This may lead to extra clean checkouts. Probably these configurations should have identical Swabra cleanup settings';
            $('clashing.warning').update(message);
            BS.Util.show($('swabra.clashing.warning.container'));
          } else {
            $('clashing.warning').update();
            BS.Util.hide($('swabra.clashing.warning.container'));
          }
        }
      });
    },

    onEnabledChange: function() {
      var enabledEl = $('swabra.enabled');
      var selectedValue = enabledEl.options[enabledEl.selectedIndex].value;

      if (selectedValue == '') {
        BS.Util.hide($('swabra.strict.container'));
        BS.Util.hide($('swabra.verbose.container'));
        BS.Util.hide($('swabra.rules.container'));
      } else {
        BS.Util.show($('swabra.strict.container'));
        BS.Util.show($('swabra.verbose.container'));
        BS.Util.show($('swabra.rules.container'));
      }

      BS.MultilineProperties.updateVisible();

      this.updateClashing(selectedValue, $('swabra.strict').checked, $('swabra.rules').value);
    },

    onProcessesChange: function() {
      var processesEl = $('swabra.processes');
      var selectedValue = processesEl.options[processesEl.selectedIndex].value;

      if (selectedValue == '') {
        BS.Util.hide($('swabra.processes.note'));
        BS.Util.hide($('swabra.processes.report.note'));
        BS.Util.hide($('swabra.processes.kill.note'));
        BS.Util.hide($('swabra.processes.handle.note'));
        BS.Util.hide($('swabra.download.handle.container'));
      } else {
        BS.Util.show($('swabra.processes.note'));
        BS.Util.show($('swabra.processes.handle.note'));
        BS.Util.show($('swabra.download.handle.container'));
        if (selectedValue == 'report') {
          BS.Util.show($('swabra.processes.report.note'));
          BS.Util.hide($('swabra.processes.kill.note'));
        } else if (selectedValue == 'kill') {
          BS.Util.hide($('swabra.processes.report.note'));
          BS.Util.show($('swabra.processes.kill.note'));
        }
      }
      BS.MultilineProperties.updateVisible();
    },

    onStrictChange: function() {
      this.updateClashing($('swabra.enabled').value, $('swabra.strict').checked, $('swabra.rules').value);
    }
  };

  BS.Swabra.updateClashing('${enabledSelected}', '${propertiesBean.properties['swabra.strict']}', '${propertiesBean.properties['swabra.rules']}');
</script>



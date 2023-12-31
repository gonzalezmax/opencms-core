<%@ page session="true" %><%--
--%><jsp:useBean id="Bean" class="org.opencms.setup.CmsUpdateBean" scope="session" /><%--
--%><jsp:setProperty name="Bean" property="*" /><%

	// next page to be accessed
	String nextPage = "step_2_settings.jsp";


%>
<%= Bean.getHtmlPart("C_HTML_START") %>
OpenCms Update-Wizard - Update Database
<%= Bean.getHtmlPart("C_HEAD_START") %>
	<script >

		var enabled = false;
		var finished = false;
		var animation;
		var message = "Updating database ... please wait";
		var countchar = 0;

		// indicates if the document has been loaded 
		function enable() {
			enabled = true;
			parent.data.location.href="step_1b_data_update.jsp";
			replaceInfo(message, "wait.gif");
		}

		// displays the given output 
		function start(out) {
			if (enabled) {
				document.forms[0].ctn.disabled = true;
				document.forms[0].bck.disabled = true;
				
				temp = "";
				for(var i=out.length-1; i>=0; i--) {
					temp += unescape(out[i])+"\n";
				}
				
				var pageBody = temp + document.forms[0].output.value;				
				var size = 163840;		
				if (pageBody.length > size) {
					pageBody = pageBody.substring(0, size);
				}
			    
				document.forms[0].output.value = pageBody;
			}
		}

		// displays a message and enables the continue button
		function finish() {
			replaceInfo("Finished. Please check the output to see if the database has been updated without errors.", "ok.png");
			document.forms[0].ctn.disabled = false;
			document.forms[0].bck.disabled = true;
			finished = true;
		}

		// if finished, you can access next page 
		function nextpage() {
			if (finished) {
				top.location.href="<%= nextPage %>";
			}
		}

		// replaces info message 
		function replaceInfo(msgString, imgSrc) {
			var el = document.getElementById("statustxt");
			var newTextNode = document.createTextNode(msgString);
			el.replaceChild(newTextNode, el.firstChild);
			el = document.getElementById("statusimg");
			el.src = "resources/" + imgSrc;
		}

	</script>
<%= Bean.getHtmlPart("C_STYLES") %>
<%= Bean.getHtmlPart("C_STYLES_SETUP") %>
<%= Bean.getHtmlPart("C_HEAD_END") %>
OpenCms Update-Wizard - Update database
<%= Bean.getHtmlPart("C_CONTENT_SETUP_START") %>

<% if (Bean.isInitialized()) { %>
<form action="<%= nextPage %>" method="post" class="nomargin">
<table border="0" cellpadding="0" cellspacing="0" style="width: 100%; height: 100%;">
<tr>
	<td style="vertical-align: middle;">
		<%= Bean.getHtmlPart("C_BLOCK_START", "Status") %>
		<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td><img src="resources/wait.gif" border="0" id="statusimg"></td>
				<td>&nbsp;&nbsp;</td>
				<td>
					<span id="statustxt">Updating database ... please wait</span>
				</td>
			</tr>
		</table>
		<%= Bean.getHtmlPart("C_BLOCK_END") %>
		<div class="dialogspacer" unselectable="on">&nbsp;</div>
	</td>
</tr>
<tr>
	<td style="vertical-align: top;">
			<textarea style="width:99%; height:260px; font-size: 11px;" cols="60" rows="16" wrap="off" name="output" id="output"></textarea>
	</td>
</tr>
</table>
<%= Bean.getHtmlPart("C_CONTENT_END") %>

<%= Bean.getHtmlPart("C_BUTTONS_START") %>
<input name="bck" id="bck" type="button" value="&#060;&#060; Back" class="dialogbutton" disabled='disabled'>
<input name="ctn" id="ctn" type="button" value="Continue &#062;&#062;" class="dialogbutton" onclick="nextpage();">
<input name="cancel" type="button" value="Cancel" class="dialogbutton" onclick="top.document.location.href='index.jsp';" style="margin-left: 50px;">
</form>
<%= Bean.getHtmlPart("C_BUTTONS_END") %>
<script >
	enable();
</script>
<% } else { %>
<%= Bean.displayError("")%>
<%= Bean.getHtmlPart("C_CONTENT_END") %>
<% } %>
<%= Bean.getHtmlPart("C_HTML_END") %>

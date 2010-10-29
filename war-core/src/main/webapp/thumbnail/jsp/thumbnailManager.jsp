<%--

    Copyright (C) 2000 - 2009 Silverpeas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    As a special exception to the terms and conditions of version 3.0 of
    the GPL, you may redistribute this Program in connection with Free/Libre
    Open Source Software ("FLOSS") applications as described in Silverpeas's
    FLOSS exception.  You should have recieved a copy of the text describing
    the FLOSS exception, and it is also available here:
    "http://repository.silverpeas.com/legal/licensing"

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@page import="com.silverpeas.thumbnail.model.ThumbnailDetail"%>
<%@ include file="thumbnailHeader.jsp"%>
<%
	
%>

<%@page import="java.net.URLEncoder"%>
<%
	String result		    = (String)request.getAttribute("resultThumbnail");
    String action		    = (String)request.getAttribute("action");
	String componentId		= request.getParameter("ComponentId");
	String objectId			= request.getParameter("ObjectId");
	String objectType		= request.getParameter("ObjectType");
	String backUrl		    = request.getParameter("BackUrl");
	String thumbnailHeight  = request.getParameter("ThumbnailHeight");
    String thumbnailWidth   = request.getParameter("ThumbnailWidth");
    
	ThumbnailSessionController thumbnailScc = (ThumbnailSessionController) request.getAttribute("thumbnail");
	ThumbnailDetail currentThumbnail = (ThumbnailDetail) request.getAttribute("thumbnaildetail");
	boolean isCreateMode = currentThumbnail == null;
	
	boolean isAddMode = true;
	boolean isUpdateFileMode = "update".equals(action);
	if(isUpdateFileMode){
		isAddMode = false;
		isCreateMode = true;
	}
	
	// case update
	String vignette_url = null;
	if(!isCreateMode){
		vignette_url = FileServer.getUrl("useless", currentThumbnail.getInstanceId(), "vignette",
											currentThumbnail.getOriginalFileName(), 
											currentThumbnail.getMimeType(),
											"images");
	}
	
	boolean error = false;
	if(result != null && !"ok".equals(result)){
		error = true;
	}
%>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/jquery/jquery-1.2.6.js"></script>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/jquery/jquery.Jcrop.js"></script>
<link type="text/css" rel="stylesheet" href="<%=m_context%>/util/styleSheets/jquery.Jcrop.css">

<SCRIPT LANGUAGE="JavaScript">

function save(){
	var path = document.thumbnailForm.OriginalFile.value;
    if(path != null && path.length > 0){
	   document.thumbnailForm.submit();
       tb_remove();
	} else {
	  alert('<%=resource.getString("thumbnail.nofile")%>');
	}
}

function saveUpdate(){
	document.thumbnailForm.submit();
    tb_remove();
}

function cancelWindow(){
	// into no have return false
  tb_remove();
}
<%if(isCreateMode){%>
function initThumbnailManager(){
	document.thumbnailForm.originalFile.focus();
}
<%}else{%>
function initThumbnailManager(){
		jQuery('#cropbox').Jcrop({
			onChange: showPreview,
			onSelect: showPreview,
			aspectRatio: <%=thumbnailWidth%>/<%=thumbnailHeight%>,
			boxWidth: 800,
			boxHeight: 330,
			setSelect : [<%=currentThumbnail.getXStart()%>,<%=currentThumbnail.getYStart()%>,<%=currentThumbnail.getXStart() + currentThumbnail.getXLength()%>,<%=currentThumbnail.getYStart() + currentThumbnail.getYLength()%>]
		});
}
<%}%>
 
// Our simple event handler, called from onChange and onSelect
// event handlers, as per the Jcrop invocation above
function showPreview(coords)
{
	var cropbox = document.getElementById('cropbox');
	if (parseInt(coords.w) > 0)
	{
		// visual calcul
		var rx = <%=thumbnailWidth%> / coords.w;
		var ry = <%=thumbnailHeight%> / coords.h;
		var thumbwidth = Math.round(rx * cropbox.width);
		var thumbheight = Math.round(ry * cropbox.height);
		var xStart = Math.round(rx * coords.x);
		var yStart = Math.round(ry * coords.y);
		 
		$('#preview').css({
			width: thumbwidth + 'px',
			height: thumbheight + 'px',
			marginLeft: '-' + xStart + 'px',
			marginTop: '-' + yStart + 'px'
		});

		// save for form
		document.thumbnailForm.XStart.value = coords.x;
		document.thumbnailForm.YStart.value = coords.y;
		document.thumbnailForm.XLength.value = coords.w;
		document.thumbnailForm.YLength.value = coords.h;
	}
};


</SCRIPT>
<center>
<FORM name="thumbnailForm" METHOD=POST action="<%=m_context%>/Thumbnail/jsp/thumbnailManager.jsp" <%if(isCreateMode){%>enctype="multipart/form-data"<%}%>>
<input type="hidden" name="ComponentId" value="<%=componentId%>">
<input type="hidden" name="ObjectId" value="<%=objectId%>">
<input type="hidden" name="ObjectType" value="<%=objectType%>">
<input type="hidden" name="BackUrl" value="<%=backUrl%>">
<%if(thumbnailHeight != null){%>
<input type="hidden" name="ThumbnailHeight" value="<%=thumbnailHeight%>">
<%}
if(thumbnailWidth != null){%>
<input type="hidden" name="ThumbnailWidth" value="<%=thumbnailWidth%>">
<%}%>
<table width="98%" border="0" cellspacing="0" cellpadding="0" class=intfdcolor4>
	<tr> 
		<td nowrap>
			<table border="0" cellspacing="0" cellpadding="5" class="contourintfdcolor" width="100%">
			<%
			if(error){
			// message d'erreur
					%>
						<tr align=center>
							<TD class="txtlibform"><%=resource.getString("thumbnail." + result)%></TD>
						</tr>
					<%
			} else if(isCreateMode){
			%>
				<tr align=center>	
					<TD class="txtlibform"><%=resource.getString("thumbnail.path")%></TD>
      				<TD>
      				<%if(isUpdateFileMode){%>
      					<input type="hidden" name="Action" value="SaveUpdateFile">
      				<%}else{%>
      					<input type="hidden" name="Action" value="Save">
      				<%}%>
						<input type="file" name="OriginalFile" size="60">
					</TD>
				</tr>
			<%
			}else{
			%>
			<tr align=center> 
					<TD class="txtlibform"><%=resource.getString("thumbnail.picture")%></TD>
      				<TD>
						<input type="hidden" name="Action" value="SaveUpdate">
						<input type="hidden" name="XStart">
						<input type="hidden" name="YStart">
						<input type="hidden" name="XLength">
						<input type="hidden" name="YLength">
						<table cellpadding="0" cellspacing="0" border="0">
							<tr>
							<td>
							<img src="<%=vignette_url%>" id="cropbox" />
							</td>
							<td>
								<div style="width:<%=thumbnailWidth%>px;height:<%=thumbnailHeight%>px;overflow:hidden;margin-left:5px;border-width:2px;border-style:solid;border-color:black;">
									<img src="<%=vignette_url%>" id="preview" />
								</div>
							</td>
							</tr>
						</table>
					</TD>
				</tr>
			<%
			}
			%>
			</table>
		</td>
	</tr>
</table>
</FORM>
<br>
<%
if(!error){
	ButtonPane buttonPane = gef.getButtonPane();
	if(isCreateMode){
	    buttonPane.addButton((Button) gef.getFormButton(resource.getString("GML.validate"), "javascript:save();", false));
	}else{
	    buttonPane.addButton((Button) gef.getFormButton(resource.getString("GML.validate"), "javascript:saveUpdate();", false));
	}
    buttonPane.addButton((Button) gef.getFormButton(resource.getString("GML.cancel"), "javascript:cancelWindow();", false));
    out.println(buttonPane.print());
}
%>
</center>
<%
if(!error){
// on lance l init vu qu on n a pas de onLoad()
%>
<SCRIPT>
	setTimeout("initThumbnailManager()", 500);
</SCRIPT>
<%
}
%>
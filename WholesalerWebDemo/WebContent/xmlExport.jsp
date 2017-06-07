<%@ page session="true" import="de.htwg_konstanz.ebus.wholesaler.demo.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<head>
    <title>eBusiness Framework Demo - Export</title>
    <meta http-equiv="cache-control" content="no-cache">
    <meta http-equiv="pragma" content="no-cache">
    <link rel="stylesheet" type="text/css" href="default.css">
</head>
<body>

<%@ include file="header.jsp" %>
<%@ include file="error.jsp" %>
<%@ include file="authentication.jsp" %>
<%@ include file="navigation.jspfragment" %>

<h1>Export</h1>
<div>
    <form name="xmlUploadForm" method="post"
          action="<%= response.encodeURL("controllerservlet?action=" + Constants.ACTION_SHOW_XML_EXPORT)%>">
        <div>
	        <label>Choose export action:
		        <select name="exportFormat">
					<option value="xml">show xml(bmecat)</option>
					<option value="xmlDownload">download xml(bmecat)</option>
					<option value="xhtml">show xhtml</option>
					<option value="xhtmlDownload">download xhtml</option>
				</select>
			</label>
		</div>
        <div>
        	<label>Search by article short description:</label>
        	<input type="text" name="search" placeholder="search"/>
			<label>(if empty whole catalog will be selected)</label>
		</div>
        <input type="submit" value="Export">
    </form>
</div>
</body>
</html>

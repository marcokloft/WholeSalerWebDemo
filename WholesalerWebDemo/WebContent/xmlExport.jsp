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

<h1>XML Export</h1>
<div>
    <form name="xmlExportForm" method="post"
          action="<%= response.encodeURL("controllerservlet?action=" + Constants.ACTION_SHOW_XML_EXPORT)%>">
        <div>
			<input type="radio" id="one" 	name="exportType" value="xml">				<label for="one"> XML</label><br> 
    		<input type="radio" id="two" 	name="exportType" value="xmlDownload">		<label for="two"> Download-XML</label><br> 
    		<input type="radio" id="three" 	name="exportType" value="xhtml">			<label for="three"> xHTML</label><br>
    		<input type="radio" id="four" 	name="exportType" value="xhtmlDownload">	<label for="four"> xHTML-Download</label> 
		</div>
        <div>        
        	<label for="search">Search by short description:</label>
        	<input type="text" name="searchTherm" placeholder="search" id="search"/>
		</div>
        <input type="submit" value="Export">
    </form>
</div>
</body>
</html>

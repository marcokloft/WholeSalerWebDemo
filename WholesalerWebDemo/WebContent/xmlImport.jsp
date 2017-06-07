<%@ page session="true" import="de.htwg_konstanz.ebus.wholesaler.demo.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>eBusiness Framework Demo - XML Import</title>
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="pragma" content="no-cache">
<link rel="stylesheet" type="text/css" href="default.css">
</head>
<body>
	<%@ include file="header.jsp"%>
	<%@ include file="error.jsp"%>
	<%@ include file="authentication.jsp"%>
	<%@ include file="navigation.jspfragment"%>

	<h1>XML Import</h1>
	<div>
		<form name="xmlImportForm" method="post"
			action="<%=response.encodeURL("controllerservlet?action=" + Constants.ACTION_SHOW_XML_IMPORT)%>"
			enctype="multipart/form-data">
			<input type="file" name="<%=Constants.PARAM_XML_FILE%>">
			<input type="submit" value="submit">
		</form>

		<p style="color: green">
			<%
				String name = (String) request.getAttribute("xmlImported");
				if (name != null) {

					out.println(name);
				}
			%>
		</p>
	</div>


</body>
</html>
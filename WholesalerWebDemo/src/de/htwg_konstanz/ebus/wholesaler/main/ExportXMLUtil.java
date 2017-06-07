package de.htwg_konstanz.ebus.wholesaler.main;

import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ExportXMLUtil {
	
	private HttpServletRequest request;
	private HttpServletResponse response;
	ServletContext context;
	private ArrayList<String> errorList;

	public ExportXMLUtil(HttpServletRequest request, HttpServletResponse response, ArrayList<String> errorList) {
		this.request = request;
		this.response = response;
		this.errorList = errorList;
		this.context = request.getSession().getServletContext();
	}

	public String exportFile(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getErrorList() {
		// TODO Auto-generated method stub
		return null;
	}

}

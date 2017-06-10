package de.htwg_konstanz.ebus.wholesaler.action;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.htwg_konstanz.ebus.framework.wholesaler.api.security.Security;
import de.htwg_konstanz.ebus.wholesaler.demo.IAction;
import de.htwg_konstanz.ebus.wholesaler.demo.LoginBean;
import de.htwg_konstanz.ebus.wholesaler.demo.util.Constants;
import de.htwg_konstanz.ebus.wholesaler.main.ExportXMLUtil;


public class ExportXMLAction implements IAction {

	public ExportXMLAction() {
		super();
	}

	@Override
	public String execute(HttpServletRequest request, HttpServletResponse response, ArrayList<String> errorList) {
		errorList.clear();
		// get the login bean from the session
		LoginBean loginBean = (LoginBean) request.getSession(true).getAttribute(Constants.PARAM_LOGIN_BEAN);

		if (loginBean != null && loginBean.isLoggedIn()) {
			// ensure that the user is allowed to execute this action
			// (authorization)
			// at this time the authorization is not fully implemented.
			// -> use the "Security.RESOURCE_ALL" constant which includes all
			// resources.
			if (Security.getInstance().isUserAllowed(loginBean.getUser(), Security.RESOURCE_ALL,
					Security.ACTION_READ)) {

				// our code
				ExportXMLUtil Export = new ExportXMLUtil(request, response, errorList);
				String userId = loginBean.getUser().getLogin();
				String pathToFile = Export.exportFile(userId);

				if (Export.getErrorList().isEmpty())
					return pathToFile;
				else
					return "xmlExport.jsp";

			} else {
				// authorization failed -> show error message
				errorList.add("You are not allowed to perform this action!");

				// redirect to the welcome page
				return "welcome.jsp";
			}
		}

		// redirect to the login page
		return "login.jsp";
	}

	@Override
	public boolean accepts(String actionName) {
		return actionName.equalsIgnoreCase(Constants.ACTION_SHOW_XML_EXPORT);
	}

}

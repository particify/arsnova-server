package de.thm.arsnova;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

public class LoginAuthenticationSucessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private String targetUrl;
	
	@Override
	protected String determineTargetUrl(HttpServletRequest request,
			HttpServletResponse response) {
		HttpSession session = request.getSession();
		if (session == null || session.getAttribute("ars-referer") == null) {
			return targetUrl;
		}
		String referer = (String) session.getAttribute("ars-referer");
		return referer + targetUrl;
	}
	
	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}
}

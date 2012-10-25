package de.thm.arsnova;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

public class CASLogoutSuccessHandler implements LogoutSuccessHandler {

	public static final Logger LOGGER = LoggerFactory.getLogger(CASLogoutSuccessHandler.class);

	private String casUrl;
	private String defaultTarget;

	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@Override
	public final void onLogoutSuccess(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Authentication authentication
	) throws IOException, ServletException {
		String referer = request.getHeader("referer");
		if (response.isCommitted()) {
			LOGGER.info("Response has already been committed. Unable to redirect to target");
			return;
		}
		redirectStrategy.sendRedirect(
				request,
				response,
				(casUrl + "/logout?url=") + (referer != null ? referer : defaultTarget)
		);
	}

	public final void setCasUrl(final String newCasUrl) {
		casUrl = newCasUrl;
	}

	public final void setDefaultTarget(final String newDefaultTarget) {
		defaultTarget = newDefaultTarget;
	}
}

package de.thm.arsnova.service.comment.security;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Component
public class JwtTokenFilter extends GenericFilterBean {
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("Bearer (.*)", Pattern.CASE_INSENSITIVE);
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);
    private JwtService jwtService;

    @Override
    public void doFilter(final ServletRequest servletRequest,
            final ServletResponse servletResponse,
            final FilterChain filterChain)
            throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        String token = null;
        final String jwtHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (jwtHeader != null) {
            final Matcher tokenMatcher = BEARER_TOKEN_PATTERN.matcher(jwtHeader);
            if (tokenMatcher.matches()) {
                token = tokenMatcher.group(1);
            } else {
                logger.debug("Unsupported authentication scheme.");
            }
        } else {
            logger.debug("No authentication header present.");
        }

        try {
            jwtService.verifyToken(token);
        } catch (final Exception e) {
            logger.debug("JWT authentication failed", e);
            httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token.");
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Autowired
    public void setJwtService(final JwtService jwtService) {
        this.jwtService = jwtService;
    }
}

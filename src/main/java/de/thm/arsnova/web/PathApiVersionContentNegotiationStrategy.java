package de.thm.arsnova.web;

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.controller.AbstractEntityController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This {@link ContentNegotiationStrategy} selects the media type based on request path. It allows to set the correct
 * media type for old clients which do not set the 'Accept' header.
 *
 * @author Daniel Gerhardt
 */
public class PathApiVersionContentNegotiationStrategy implements ContentNegotiationStrategy {
	private static final Logger logger = LoggerFactory.getLogger(PathApiVersionContentNegotiationStrategy.class);

	private MediaType fallback;
	private MediaType empty = MediaType.valueOf(AbstractEntityController.MEDIATYPE_EMPTY);

	public PathApiVersionContentNegotiationStrategy(MediaType fallback) {
		this.fallback = fallback;
	}

	@Override
	public List<MediaType> resolveMediaTypes(final NativeWebRequest webRequest)
			throws HttpMediaTypeNotAcceptableException {
		final HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		final List<MediaType> mediaTypes = new ArrayList<>();
		if (servletRequest.getServletPath().startsWith("/v2/")) {
			logger.trace("Negotiating content based on path for API v2");
			mediaTypes.add(AppConfig.API_V2_MEDIA_TYPE);
			mediaTypes.add(MediaType.TEXT_PLAIN);
		} else {
			logger.trace("Content negotiation falling back to {}", fallback);
			if (servletRequest.getHeader(HttpHeaders.ACCEPT) == null
					&& Arrays.asList("POST", "PUT", "PATCH").contains(servletRequest.getMethod())) {
				/* This allows AbstractEntityController to send an empty response if no Accept header is set */
				logger.debug("No Accept header present for {} request. Entity will not be sent in response",
						servletRequest.getMethod());
				mediaTypes.add(empty);
			} else {
				mediaTypes.add(fallback);
			}
		}

		return mediaTypes;
	}
}

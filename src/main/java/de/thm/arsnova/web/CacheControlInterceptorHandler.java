package de.thm.arsnova.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class CacheControlInterceptorHandler extends HandlerInterceptorAdapter {

	@Override
	public boolean preHandle(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler) throws Exception {

		setCacheControlResponseHeader(request, response, handler);
		return super.preHandle(request, response, handler);
	}

	private void setCacheControlResponseHeader(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler) {

		CacheControl cacheControl = getCacheControlAnnotation(request, response, handler);

		if (cacheControl == null) {
			return;
		}

		StringBuilder headerValue = new StringBuilder();

		if(cacheControl.policy().length > 0) {
			for (CacheControl.Policy policy : cacheControl.policy()) {
				if (headerValue.length() > 0) {
					headerValue.append(", ");
				}
				headerValue.append(policy.getPolicyString());
			}
		}

		if (cacheControl.noCache()) {
			if (headerValue.length() > 0) {
				headerValue.append(", ");
			}
			headerValue.append("max-age=0, no-cache");
			response.setHeader("cache-control", headerValue.toString());
		}

		if(cacheControl.maxAge() >= 0) {
			if (headerValue.length() > 0) {
				headerValue.append(", ");
			}
			headerValue.append("max-age=").append(cacheControl.maxAge());
		}

		response.setHeader("cache-control", headerValue.toString());
	}

	private CacheControl getCacheControlAnnotation(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler
			) {
		if (handler == null || !(handler instanceof HandlerMethod)) {
			return null;
		}

		final HandlerMethod handlerMethod = (HandlerMethod) handler;
		return handlerMethod.getMethodAnnotation(CacheControl.class);
	}
}

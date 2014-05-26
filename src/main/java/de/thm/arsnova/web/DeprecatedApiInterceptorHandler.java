package de.thm.arsnova.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class DeprecatedApiInterceptorHandler extends HandlerInterceptorAdapter {

	@Override
	public boolean preHandle(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler) throws Exception {

		if (getDeprecatedApiAnnotation(request, response, handler) != null) {
			response.addHeader("X-Deprecated-Api", "1");
		}

		return super.preHandle(request, response, handler);
	}

	private DeprecatedApi getDeprecatedApiAnnotation(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler
			) {
		if (handler == null || !(handler instanceof HandlerMethod)) {
			return null;
		}

		final HandlerMethod handlerMethod = (HandlerMethod) handler;
		return handlerMethod.getMethodAnnotation(DeprecatedApi.class);
	}
}

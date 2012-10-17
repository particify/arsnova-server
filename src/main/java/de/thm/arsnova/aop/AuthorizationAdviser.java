package de.thm.arsnova.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;

import de.thm.arsnova.annotation.Authenticated;

@Aspect
public class AuthorizationAdviser {

	@Pointcut("execution(public * de.thm.arsnova.services.*.*(..))")
	public void serviceMethods() {}
	
	@Autowired
	@Before("serviceMethods() && @annotation(authenticated) && this(object)")
	public void check(Authenticated authenticated, Object object) {
		System.out.println("******************************: " + object.getClass().getName());
	}
}

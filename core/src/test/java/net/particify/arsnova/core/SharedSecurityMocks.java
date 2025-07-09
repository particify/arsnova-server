package net.particify.arsnova.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import net.particify.arsnova.core.security.CasUserDetailsService;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {
  CasUserDetailsService.class,
  DaoAuthenticationProvider.class
})
public @interface SharedSecurityMocks {
}

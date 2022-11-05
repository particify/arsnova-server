package net.particify.arsnova.comments.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import net.particify.arsnova.comments.config.properties.SecurityProperties;
import net.particify.arsnova.comments.security.JwtAuthenticationProvider;
import net.particify.arsnova.comments.security.JwtTokenFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
  private JwtTokenFilter jwtTokenFilter;
  private String managementPath;

  SecurityConfig(final WebEndpointProperties webEndpointProperties) {
    this.managementPath = webEndpointProperties.getBasePath();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    // We don't need CSRF for this example
    httpSecurity.csrf().disable();
    //httpSecurity.authorizeRequests().anyRequest().authenticated();
    httpSecurity.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    // Add a filter to validate the tokens with every request
    httpSecurity.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
    httpSecurity.authorizeRequests()
        .antMatchers(managementPath + "/**").hasAnyRole("ADMIN", "MONITORING");

      return httpSecurity.build();
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    return new ProviderManager(jwtAuthenticationProvider());
  }

  @Bean
  public JwtAuthenticationProvider jwtAuthenticationProvider() {
    return new JwtAuthenticationProvider();
  }

  @Autowired
  @Lazy
  public void setJwtTokenFilter(final JwtTokenFilter jwtTokenFilter) {
    this.jwtTokenFilter = jwtTokenFilter;
  }
}

package net.particify.arsnova.authz

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ComponentScan(basePackages = ["net.particify.arsnova.authz", "net.particify.arsnova.common"])
@EnableScheduling
class AuthzApplication : SpringBootServletInitializer() {
  override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
    return application.sources(AuthzApplication::class.java)
  }
}

fun main(args: Array<String>) {
  SpringApplication.run(AuthzApplication::class.java, *args)
}

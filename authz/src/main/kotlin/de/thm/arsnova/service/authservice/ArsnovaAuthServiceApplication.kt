package de.thm.arsnova.service.authservice

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ArsnovaAuthServiceApplication : SpringBootServletInitializer() {

  override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
    return application.sources(ArsnovaAuthServiceApplication::class.java)
  }
}

fun main(args: Array<String>) {
  SpringApplication.run(ArsnovaAuthServiceApplication::class.java, *args)
}

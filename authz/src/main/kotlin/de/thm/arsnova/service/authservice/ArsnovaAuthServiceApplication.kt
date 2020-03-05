package de.thm.arsnova.service.authservice

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
class AuthServiceApplication : SpringBootServletInitializer() {

	override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
		return application.sources(AuthServiceApplication::class.java!!)
	}

}

fun main(args: Array<String>) {
	SpringApplication.run(AuthServiceApplication::class.java, *args)
}

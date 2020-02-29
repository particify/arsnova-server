package de.thm.arsnova.service.wsgateway

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.PropertySource

@SpringBootApplication
@PropertySource(
		value = *arrayOf("classpath:arsnova.wsgateway.properties.example", "file:/etc/arsnova/arsnova.wsgateway.properties"),
		ignoreResourceNotFound = true,
		encoding = "UTF-8"
)
class ArsnovaWsGatewayServiceApplication : SpringBootServletInitializer() {

	override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
		return application.sources(ArsnovaWsGatewayServiceApplication::class.java!!)
	}

}

fun main(args: Array<String>) {
	SpringApplication.run(ArsnovaWsGatewayServiceApplication::class.java, *args)
}

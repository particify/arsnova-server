package de.thm.arsnova.service.httpgateway

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class ArsnovaHttpGatewayServiceApplication

fun main(args: Array<String>) {
    SpringApplication.run(ArsnovaHttpGatewayServiceApplication::class.java, *args)
}

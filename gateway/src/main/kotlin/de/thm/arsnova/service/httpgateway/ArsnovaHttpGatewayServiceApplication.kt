package de.thm.arsnova.service.httpgateway

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import reactor.tools.agent.ReactorDebugAgent

@SpringBootApplication
class ArsnovaHttpGatewayServiceApplication

fun main(args: Array<String>) {
    ReactorDebugAgent.init()
    SpringApplication.run(ArsnovaHttpGatewayServiceApplication::class.java, *args)
}

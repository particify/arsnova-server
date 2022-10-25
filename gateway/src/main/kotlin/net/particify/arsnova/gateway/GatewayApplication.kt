package net.particify.arsnova.gateway

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import reactor.tools.agent.ReactorDebugAgent

@SpringBootApplication
class GatewayApplication

fun main(args: Array<String>) {
  ReactorDebugAgent.init()
  SpringApplication.run(GatewayApplication::class.java, *args)
}

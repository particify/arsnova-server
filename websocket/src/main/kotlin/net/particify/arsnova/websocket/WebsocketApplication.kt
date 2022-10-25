package net.particify.arsnova.websocket

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
class WebsocketApplication : SpringBootServletInitializer() {

  override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
    return application.sources(WebsocketApplication::class.java!!)
  }
}

fun main(args: Array<String>) {
  SpringApplication.run(WebsocketApplication::class.java, *args)
}

package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.exception.ForbiddenException
import de.thm.arsnova.service.httpgateway.model.CommentServiceStats
import de.thm.arsnova.service.httpgateway.model.Stats
import de.thm.arsnova.service.httpgateway.model.WsGatewayStats
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import de.thm.arsnova.service.httpgateway.service.CommentService
import de.thm.arsnova.service.httpgateway.service.WsGatewayService
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

@Component
class SystemView(
    private val authProcessor: AuthProcessor,
    private val wsGatewayService: WsGatewayService,
    private val commentService: CommentService
) {
    fun getServiceStats(): Mono<Stats> {
        return authProcessor.getAuthentication()
            .filter { authentication: Authentication ->
                authProcessor.isAdminOrMonitoring(authentication)
            }
            .switchIfEmpty(Mono.error(ForbiddenException()))
            .flatMap {
                Mono.zip(
                    wsGatewayService.getGatewayStats(),
                    commentService.getServiceStats()
                )
                    .map { tuple2: Tuple2<WsGatewayStats, CommentServiceStats> ->
                        Stats(tuple2.t1, tuple2.t2)
                    }
            }
    }
}

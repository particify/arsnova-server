package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.exception.ForbiddenException
import de.thm.arsnova.service.httpgateway.model.CommentServiceStats
import de.thm.arsnova.service.httpgateway.model.Stats
import de.thm.arsnova.service.httpgateway.model.WsGatewayStats
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import de.thm.arsnova.service.httpgateway.service.CommentService
import de.thm.arsnova.service.httpgateway.service.CoreStatsService
import de.thm.arsnova.service.httpgateway.service.WsGatewayService
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.util.function.Tuple3

@Component
class SystemView(
    private val authProcessor: AuthProcessor,
    private val wsGatewayService: WsGatewayService,
    private val coreStatsService: CoreStatsService,
    private val commentService: CommentService
) {
    fun getServiceStats(): Mono<Stats> {
        return authProcessor.getAuthentication()
            .filter { authentication: Authentication ->
                authProcessor.isAdminOrMonitoring(authentication)
            }
            .switchIfEmpty(Mono.error(ForbiddenException()))
            .map { authentication ->
                authentication.credentials
            }
            .cast(String::class.java)
            .flatMap { jwt ->
                Mono.zip(
                    wsGatewayService.getGatewayStats(),
                    coreStatsService.getServiceStats(jwt),
                    commentService.getServiceStats()
                )
                    .map { tuple3: Tuple3<WsGatewayStats, Map<String, Any>, CommentServiceStats> ->
                        Stats(tuple3.t1, tuple3.t2, tuple3.t3)
                    }
            }
    }
}

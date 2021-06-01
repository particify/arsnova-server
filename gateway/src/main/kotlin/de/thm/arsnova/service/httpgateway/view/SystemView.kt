package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.exception.ForbiddenException
import de.thm.arsnova.service.httpgateway.model.CommentServiceStats
import de.thm.arsnova.service.httpgateway.model.CoreStats
import de.thm.arsnova.service.httpgateway.model.Stats
import de.thm.arsnova.service.httpgateway.model.SummarizedStats
import de.thm.arsnova.service.httpgateway.model.WsGatewayStats
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import de.thm.arsnova.service.httpgateway.service.CommentService
import de.thm.arsnova.service.httpgateway.service.CoreStatsService
import de.thm.arsnova.service.httpgateway.service.WsGatewayService
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.kotlin.core.util.function.component3

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
                    .map { (wsGatewayStats: WsGatewayStats, coreServiceStats: Map<String, Any>, commentServiceStats: CommentServiceStats) ->
                        Stats(wsGatewayStats, coreServiceStats, commentServiceStats)
                    }
            }
    }

    fun getSummarizedStats(): Mono<SummarizedStats> {
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
                            coreStatsService.getSummarizedStats(jwt),
                            commentService.getServiceStats()
                    )
                            .map { (wsGatewayStats: WsGatewayStats, coreStats: CoreStats, commentServiceStats: CommentServiceStats) ->
                                SummarizedStats(
                                        connectedUsers = wsGatewayStats.webSocketUserCount,
                                        users = coreStats.userProfile.accountCount,
                                        activationsPending = coreStats.userProfile.activationsPending,
                                        rooms = coreStats.room.totalCount,
                                        contents = coreStats.content.totalCount,
                                        answers = coreStats.answer.totalCount,
                                        comments = commentServiceStats.commentCount.toInt()
                                )
                            }
                }
    }
}

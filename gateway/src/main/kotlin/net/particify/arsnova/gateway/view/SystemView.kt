package net.particify.arsnova.gateway.view

import net.particify.arsnova.gateway.exception.ForbiddenException
import net.particify.arsnova.gateway.model.CommentServiceStats
import net.particify.arsnova.gateway.model.CoreStats
import net.particify.arsnova.gateway.model.Stats
import net.particify.arsnova.gateway.model.SummarizedStats
import net.particify.arsnova.gateway.model.WsGatewayStats
import net.particify.arsnova.gateway.security.AuthProcessor
import net.particify.arsnova.gateway.service.CommentService
import net.particify.arsnova.gateway.service.CoreStatsService
import net.particify.arsnova.gateway.service.WsGatewayService
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

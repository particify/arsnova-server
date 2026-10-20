/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.execution.SubscriptionExceptionResolverAdapter
import org.springframework.stereotype.Component

/**
 * Classifies the errors ending a subscription, which [QuestionnaireExceptionResolverAdapter] does
 * not see: it only handles those raised while the subscription is set up.
 */
@Component
class QuestionnaireSubscriptionExceptionResolver : SubscriptionExceptionResolverAdapter() {
  override fun resolveToSingleError(exception: Throwable): GraphQLError? {
    val message = notFoundMessage(exception) ?: return null
    return GraphqlErrorBuilder.newError().errorType(ErrorType.NOT_FOUND).message(message).build()
  }
}

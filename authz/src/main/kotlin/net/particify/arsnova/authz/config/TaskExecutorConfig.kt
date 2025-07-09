package net.particify.arsnova.authz.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class TaskExecutorConfig {
  @Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
  )
  @Retention(AnnotationRetention.RUNTIME)
  @Qualifier
  annotation class RabbitConnectionExecutor

  @Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
  )
  @Retention(AnnotationRetention.RUNTIME)
  @Qualifier
  annotation class RabbitListenerExecutor

  /**
   * "The executor’s thread pool should be unbounded, or set appropriately for the expected utilization (usually, at least one thread per connection). If multiple channels are created on each connection then the pool size will affect the concurrency, so a variable (or simple cached) thread pool executor would be most suitable."
   *
   * Reference:
   * http://docs.spring.io/spring-amqp/reference/htmlsingle/#connections
   */
  @Bean
  @RabbitConnectionExecutor
  fun rabbitConnectionExecutor(): TaskExecutor? {
    val executor = ThreadPoolTaskExecutor()
    executor.setThreadNamePrefix("RabbitConnection")
    executor.afterPropertiesSet()
    return executor
  }

  /**
   * Listeners would use a SimpleAsyncTaskExecutor by default (creates a new thread for each task).
   *
   * Reference:
   * http://docs.spring.io/spring-amqp/reference/htmlsingle/#_threading_and_asynchronous_consumers
   */
  @Bean
  @RabbitListenerExecutor
  fun rabbitListenerExecutor(): TaskExecutor? {
    val executor = ThreadPoolTaskExecutor()
    executor.setThreadNamePrefix("RabbitListener")
    executor.afterPropertiesSet()
    return executor
  }
}

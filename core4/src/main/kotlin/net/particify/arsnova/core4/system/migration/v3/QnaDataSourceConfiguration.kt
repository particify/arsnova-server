/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = ["persistence.v3-migration.enabled"])
class QnaDataSourceConfiguration {
  @Bean(defaultCandidate = false)
  @ConfigurationProperties("persistence.v3-migration.qna.datasource")
  fun legacyQnaDataSourceProperties(): DataSourceProperties {
    return DataSourceProperties()
  }

  @Bean(defaultCandidate = false)
  fun legacyQnaDataSource(
      @Qualifier("legacyQnaDataSourceProperties") properties: DataSourceProperties
  ): HikariDataSource {
    return properties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
  }
}

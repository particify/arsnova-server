package net.particify.arsnova.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import net.particify.arsnova.core.config.TestAppConfig;
import net.particify.arsnova.core.config.TestPersistanceConfig;
import net.particify.arsnova.core.config.TestSecurityConfig;

@SpringBootTest
@Import({
    TestAppConfig.class,
    TestPersistanceConfig.class,
    TestSecurityConfig.class})
@ActiveProfiles("test")
class CoreApplicationTests {

  @Test
  void contextLoads() {
  }

}

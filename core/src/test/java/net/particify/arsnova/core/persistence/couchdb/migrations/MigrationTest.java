package net.particify.arsnova.core.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.ektorp.CouchDbInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import net.particify.arsnova.core.config.TestAppConfig;
import net.particify.arsnova.core.config.TestPersistanceConfig;
import net.particify.arsnova.core.config.TestSecurityConfig;
import net.particify.arsnova.core.model.serialization.CouchDbObjectMapperFactory;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

@SpringBootTest
@Import({
  TestAppConfig.class,
  TestPersistanceConfig.class,
  TestSecurityConfig.class})
@ActiveProfiles("test")
public class MigrationTest {
  @MockitoBean
  public CouchDbInstance couchDbInstance;

  @Test
  public void testMigrationEntitySerialization() throws JsonProcessingException {
    final var connector = new MangoCouchDbConnector("test", couchDbInstance);
    final var om = (new CouchDbObjectMapperFactory()).createObjectMapper(connector);
    final var entity = new TestMigrationEntity();
    entity.testProperty = "testValue";
    entity.setProperty("unmappedProperty", "unmappedValue");
    final var json = om.writerFor(TestMigrationEntity.class).writeValueAsString(entity);
    final TestMigrationEntity deserializedEntity = om.readerFor(TestMigrationEntity.class).readValue(json);
    Assertions.assertEquals(entity.testProperty, deserializedEntity.testProperty);
    final var properties = deserializedEntity.getProperties();
    Assertions.assertTrue(properties.containsKey("unmappedProperty"));
    Assertions.assertEquals("unmappedValue", properties.get("unmappedProperty"));
  }

  private static class TestMigrationEntity extends MigrationEntity {
    private String testProperty;

    @JsonView(View.Persistence.class)
    public String getTestProperty() {
      return testProperty;
    }

    @JsonView(View.Persistence.class)
    public void setTestProperty(final String testProperty) {
      this.testProperty = testProperty;
    }
  }
}

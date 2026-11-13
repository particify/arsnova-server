package net.particify.arsnova.core.persistence.couchdb.migrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.ektorp.CouchDbInstance;
import org.ektorp.impl.BulkDocumentWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import net.particify.arsnova.core.model.serialization.CouchDbObjectMapperFactory;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;
import net.particify.arsnova.core.persistence.couchdb.support.MangoQueryResultParser;

/**
 * Unmapped properties of migration entities must survive a read/write cycle,
 * see https://gitlab.com/particify/dev/foss/arsnova-server/-/issues/317.
 */
public class MigrationEntityAnyPropertiesTest {
  private static final String REAL_ENTITY =
      UserProfileRoomHistoryMigration.class.getName() + "$UserProfileMigrationEntity";
  private static final String REAL_DOC = """
      {"_id":"u1","_rev":"1-a","type":"UserProfile","loginId":"someone",
      "settings":{"a":1},"roomHistory":[{"roomId":"r1","lastVisit":"2020-01-01"}]}""";
  private static final String SCALE_ENTITY =
      ScaleContentTemplateMigration.class.getName() + "$ContentMigrationEntity";
  private static final String SCALE_DOC = """
      {"_id":"c1","_rev":"1-a","type":"Content","format":"SCALE","body":"b",
      "options":[{"label":"l","points":3}],"correctOptionIndexes":[0],"optionCount":2}""";
  private static final String CLEANUP_ROOM_ENTITY =
      ModelCleanupMigration.class.getName() + "$RoomMigrationEntity";
  private static final String CLEANUP_ROOM_DOC = """
      {"_id":"r1","_rev":"1-a","type":"Room","name":"n","abbreviation":"abbr",
      "moderators":[{"userId":"m1"}],"settings":{"commentsEnabled":true,"feedbackLocked":true}}""";
  private static final String TEST_DOC = """
      {"_id":"t1","_rev":"1-a","type":"Test","known":"k","unknown":"u",
      "inner":{"innerKnown":"ik","innerUnknown":"iu"}}""";

  private ObjectMapper om;

  @BeforeEach
  void setUp() {
    final var connector = new MangoCouchDbConnector("test", Mockito.mock(CouchDbInstance.class));
    om = new CouchDbObjectMapperFactory().createObjectMapper(connector);
  }

  @Test
  void realEntityMapperRoundTrip() throws Exception {
    final Object entity = om.readValue(REAL_DOC, Class.forName(REAL_ENTITY));
    assertRealDoc(om.readTree(om.writeValueAsString(entity)));
  }

  @Test
  void realEntityMangoReadAndBulkWrite() throws Exception {
    assertRealDoc(readAndBulkWrite(REAL_DOC, Class.forName(REAL_ENTITY)));
  }

  @Test
  void realEntityWithoutClassViewMapperRoundTrip() throws Exception {
    final Object entity = om.readValue(SCALE_DOC, Class.forName(SCALE_ENTITY));
    assertScaleDoc(om.readTree(om.writeValueAsString(entity)));
  }

  @Test
  void realEntityWithoutClassViewMangoReadAndBulkWrite() throws Exception {
    assertScaleDoc(readAndBulkWrite(SCALE_DOC, Class.forName(SCALE_ENTITY)));
  }

  @Test
  void legacyPropertyMapperRoundTrip() throws Exception {
    final Object entity = om.readValue(CLEANUP_ROOM_DOC, Class.forName(CLEANUP_ROOM_ENTITY));
    assertLegacyReadable(entity);
    assertCleanupRoomDoc(om.readTree(om.writeValueAsString(entity)));
  }

  @Test
  void legacyPropertyMangoReadAndBulkWrite() throws Exception {
    final Object entity = readViaMango(CLEANUP_ROOM_DOC, Class.forName(CLEANUP_ROOM_ENTITY));
    assertLegacyReadable(entity);
    assertCleanupRoomDoc(bulkWrite(entity));
  }

  @Test
  void testEntityMapperRoundTrip() throws Exception {
    final Object entity = om.readValue(TEST_DOC, TestEntity.class);
    assertTestDoc(om.readTree(om.writeValueAsString(entity)));
  }

  @Test
  void testEntityMangoReadAndBulkWrite() throws Exception {
    assertTestDoc(readAndBulkWrite(TEST_DOC, TestEntity.class));
  }

  /** Mirrors AbstractMigration: MangoCouchDbConnector.queryForPage, then executeBulk. */
  private <T> JsonNode readAndBulkWrite(final String doc, final Class<T> type) throws Exception {
    return bulkWrite(readViaMango(doc, type));
  }

  private <T> T readViaMango(final String doc, final Class<T> type) throws Exception {
    final String response = "{\"docs\":[" + doc + "],\"bookmark\":\"b\"}";
    final var parser = new MangoQueryResultParser<>(type, om);
    parser.parseResult(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
    final List<T> docs = parser.getDocs();
    assertEquals(1, docs.size());
    return docs.get(0);
  }

  private JsonNode bulkWrite(final Object entity) throws Exception {
    final var out = new ByteArrayOutputStream();
    new BulkDocumentWriter(om).write(List.of(entity), false, out);
    final JsonNode bulk = om.readTree(out.toByteArray());
    return bulk.get("docs").get(0);
  }

  /* The entity classes are private, so their legacy getters are reached reflectively. */
  private static Object invokeGetter(final Object target, final String name) throws Exception {
    final Method getter = target.getClass().getMethod(name);
    getter.setAccessible(true);
    return getter.invoke(target);
  }

  private static void assertLegacyReadable(final Object room) throws Exception {
    assertEquals("abbr", invokeGetter(room, "getAbbreviation"));
    assertEquals(List.of(Map.of("userId", "m1")), invokeGetter(room, "getModerators"));
    assertEquals(true, invokeGetter(invokeGetter(room, "getSettings"), "isCommentsEnabled"));
    final var properties = ((MigrationEntity) room).getProperties();
    assertFalse(properties.containsKey("abbreviation"), properties.toString());
    assertFalse(properties.containsKey("moderators"), properties.toString());
  }

  private static void assertCleanupRoomDoc(final JsonNode json) {
    assertEquals("r1", json.path("_id").asText(), json.toString());
    assertFalse(json.has("abbreviation"), "legacy property written: " + json);
    assertFalse(json.has("moderators"), "legacy property written: " + json);
    assertFalse(json.path("settings").has("commentsEnabled"), "inner legacy property written: " + json);
    assertEquals("Room", json.path("type").asText(), "top-level unmapped: " + json);
    assertEquals("n", json.path("name").asText(), "top-level unmapped: " + json);
    assertEquals(true, json.path("settings").path("feedbackLocked").asBoolean(), "inner unmapped: " + json);
  }

  private static void assertRealDoc(final JsonNode json) {
    assertEquals("u1", json.path("_id").asText(), json.toString());
    assertEquals("r1", json.path("roomHistory").path(0).path("roomId").asText(), json.toString());
    assertEquals("UserProfile", json.path("type").asText(), "top-level unmapped: " + json);
    assertEquals("someone", json.path("loginId").asText(), "top-level unmapped: " + json);
    assertEquals(1, json.path("settings").path("a").asInt(), "top-level unmapped: " + json);
    assertEquals("2020-01-01", json.path("roomHistory").path(0).path("lastVisit").asText(),
        "inner unmapped: " + json);
  }

  private static void assertScaleDoc(final JsonNode json) {
    assertEquals("c1", json.path("_id").asText(), json.toString());
    assertEquals("SCALE", json.path("format").asText(), json.toString());
    assertEquals("l", json.path("options").path(0).path("label").asText(), json.toString());
    assertEquals("Content", json.path("type").asText(), "top-level unmapped: " + json);
    assertEquals("b", json.path("body").asText(), "top-level unmapped: " + json);
    assertEquals(3, json.path("options").path(0).path("points").asInt(), "inner unmapped: " + json);
  }

  private static void assertTestDoc(final JsonNode json) {
    assertEquals("t1", json.path("_id").asText(), json.toString());
    assertEquals("k", json.path("known").asText(), json.toString());
    assertEquals("ik", json.path("inner").path("innerKnown").asText(), json.toString());
    assertEquals("u", json.path("unknown").asText(), "top-level unmapped: " + json);
    assertEquals("Test", json.path("type").asText(), "top-level unmapped: " + json);
    assertEquals("iu", json.path("inner").path("innerUnknown").asText(), "inner unmapped: " + json);
  }

  /** Like MigrationTest's entity: no class-level view. */
  public static class TestEntity extends MigrationEntity {
    private String known;
    private TestInner inner;

    @JsonView(View.Persistence.class)
    public String getKnown() {
      return known;
    }

    @JsonView(View.Persistence.class)
    public void setKnown(final String known) {
      this.known = known;
    }

    @JsonView(View.Persistence.class)
    public TestInner getInner() {
      return inner;
    }

    @JsonView(View.Persistence.class)
    public void setInner(final TestInner inner) {
      this.inner = inner;
    }
  }

  public static class TestInner extends InnerMigrationEntity {
    private String innerKnown;

    @JsonView(View.Persistence.class)
    public String getInnerKnown() {
      return innerKnown;
    }

    @JsonView(View.Persistence.class)
    public void setInnerKnown(final String innerKnown) {
      this.innerKnown = innerKnown;
    }
  }
}

package net.particify.arsnova.core.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import net.particify.arsnova.core.SharedRepositoryMocks;
import net.particify.arsnova.core.SharedSecurityMocks;
import net.particify.arsnova.core.config.AppConfig;
import net.particify.arsnova.core.config.TestAppConfig;
import net.particify.arsnova.core.config.TestPersistanceConfig;
import net.particify.arsnova.core.config.TestSecurityConfig;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.Statistics;
import net.particify.arsnova.core.persistence.RoomRepository;
import net.particify.arsnova.core.persistence.StatisticsRepository;
import net.particify.arsnova.core.service.StubAuthenticationService;
import net.particify.arsnova.core.test.context.support.WithMockUser;

/**
 * Verifies content negotiation for the management API and makes sure that the
 * media types used by Actuator cannot be used to bypass the default
 * serialization view for entities.
 */
@SpringBootTest
@Import({
    TestAppConfig.class,
    TestPersistanceConfig.class,
    TestSecurityConfig.class})
@SharedRepositoryMocks
@SharedSecurityMocks
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ActiveProfiles("test")
public class ContentNegotiationTest {
  private static final String ACTUATOR_V2_JSON = "application/vnd.spring-boot.actuator.v2+json";
  private static final String ACTUATOR_V3_JSON = "application/vnd.spring-boot.actuator.v3+json";
  private static final String JSON_CONTENT_TYPE_PATTERN = "application/([^;]+\\+)?json(;.*)?";
  private static final String ROOM_ID = "TestRoomID";
  private static final String OWNER_ID = "SecretOwnerId";
  private static final String PASSWORD = "SecretPassword";

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private StubAuthenticationService stubAuthenticationService;

  @Autowired
  private StatisticsRepository statisticsRepository;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

  @Autowired
  private MappingJackson2HttpMessageConverter defaultJsonMessageConverter;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    Mockito.reset(statisticsRepository, roomRepository);
    stubAuthenticationService.setUserAuthenticated(true, "TestUser", "1234");
  }

  /** Accept headers which are expected to result in a JSON response for the management API. */
  static Stream<String> managementAcceptHeaders() {
    return Stream.of(
        null,
        "*/*",
        MediaType.APPLICATION_JSON_VALUE,
        ACTUATOR_V2_JSON,
        ACTUATOR_V3_JSON,
        ACTUATOR_V2_JSON + "," + MediaType.APPLICATION_JSON_VALUE,
        ACTUATOR_V3_JSON + "," + ACTUATOR_V2_JSON + "," + MediaType.APPLICATION_JSON_VALUE,
        "text/html,application/xhtml+xml,*/*;q=0.8");
  }

  @ParameterizedTest
  @MethodSource("managementAcceptHeaders")
  @WithMockUser(value = "TestUser", roles = {"USER", "ADMIN"})
  public void shouldRespondWithJsonForStatsEndpoint(final String accept) throws Exception {
    final Statistics statistics = new Statistics();
    final Statistics.UserProfileStats userProfileStats = new Statistics.UserProfileStats();
    userProfileStats.setTotalCount(42);
    statistics.setUserProfile(userProfileStats);
    when(statisticsRepository.getStatistics()).thenReturn(statistics);

    mockMvc.perform(withAccept(get("/management/stats"), accept))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, matchesRegex(JSON_CONTENT_TYPE_PATTERN)))
        .andExpect(content().string(containsString("\"totalCount\":42")));
  }

  /* Statistics are serialized by Actuator's object mapper, which unlike the default mapper keeps empty maps. */
  @Test
  @WithMockUser(value = "TestUser", roles = {"USER", "ADMIN"})
  public void shouldSerializeStatsWithActuatorObjectMapper() throws Exception {
    when(statisticsRepository.getStatistics()).thenReturn(new Statistics());

    mockMvc.perform(get("/management/stats").header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"countByAuthProvider\":{}")));
  }

  @ParameterizedTest
  @MethodSource("managementAcceptHeaders")
  public void shouldRespondWithJsonForHealthEndpoint(final String accept) throws Exception {
    /* Health status depends on the test environment, so only its presence is verified. */
    mockMvc.perform(withAccept(get("/management/health"), accept))
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, matchesRegex(JSON_CONTENT_TYPE_PATTERN)))
        .andExpect(content().string(containsString("\"status\":")));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "text/plain;version=0.0.4;q=0.5,*/*;q=0.1",
      "application/openmetrics-text;version=1.0.0,text/plain;version=0.0.4;q=0.5,*/*;q=0.1"})
  @WithMockUser(value = "TestUser", roles = {"USER", "ADMIN"})
  public void shouldRespondWithTextForPrometheusEndpoint(final String accept) throws Exception {
    mockMvc.perform(withAccept(get("/management/prometheus"), accept))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
            matchesRegex("(text/plain|application/openmetrics-text);.*")))
        .andExpect(content().string(containsString("# TYPE ")));
  }

  /** Accept headers which are expected to be served with the default serialization view for entities. */
  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "*/*",
      MediaType.APPLICATION_JSON_VALUE,
      AppConfig.API_V3_MEDIA_TYPE_VALUE,
      ACTUATOR_V2_JSON + "," + MediaType.APPLICATION_JSON_VALUE})
  @WithMockUser("TestUser")
  public void shouldApplyPublicViewForEntity(final String accept) throws Exception {
    when(roomRepository.findOne(ROOM_ID)).thenReturn(createRoom());

    mockMvc.perform(withAccept(get("/room/" + ROOM_ID), accept.isEmpty() ? null : accept))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"name\":\"Test Room\"")))
        .andExpect(content().string(not(containsString(OWNER_ID))))
        .andExpect(content().string(not(containsString(PASSWORD))));
  }

  /** Accept headers for JSON media types which are not supported for entities. */
  @ParameterizedTest
  @ValueSource(strings = {
      ACTUATOR_V2_JSON,
      ACTUATOR_V3_JSON,
      "application/foo+json"})
  @WithMockUser("TestUser")
  public void shouldRejectUnsupportedJsonMediaTypesForEntity(final String accept) throws Exception {
    when(roomRepository.findOne(ROOM_ID)).thenReturn(createRoom());

    mockMvc.perform(withAccept(get("/room/" + ROOM_ID), accept))
        .andExpect(status().isNotAcceptable())
        .andExpect(content().string(not(containsString(OWNER_ID))))
        .andExpect(content().string(not(containsString(PASSWORD))));
  }

  /* Entities must only be serialized by the JSON converter which applies the Public view by default. */
  @Test
  public void shouldOnlyAllowDefaultJsonConverterToWriteEntities() {
    final List<MediaType> mediaTypes = Stream.of(
        "*/*",
        MediaType.APPLICATION_JSON_VALUE,
        ACTUATOR_V2_JSON,
        "application/foo+json",
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_XML_VALUE,
        MediaType.TEXT_PLAIN_VALUE,
        MediaType.APPLICATION_CBOR_VALUE,
        "application/x-jackson-smile",
        MediaType.APPLICATION_YAML_VALUE,
        MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .map(MediaType::valueOf)
        .toList();
    for (final HttpMessageConverter<?> converter : requestMappingHandlerAdapter.getMessageConverters()) {
      if (converter == defaultJsonMessageConverter) {
        continue;
      }
      assertFalse(converter.canWrite(Room.class, null), converter.getClass().getName());
      for (final MediaType mediaType : mediaTypes) {
        assertFalse(converter.canWrite(Room.class, mediaType), converter.getClass().getName() + " " + mediaType);
      }
    }
    assertTrue(requestMappingHandlerAdapter.getMessageConverters().contains(defaultJsonMessageConverter));
  }

  private MockHttpServletRequestBuilder withAccept(
      final MockHttpServletRequestBuilder requestBuilder, final String accept) {
    return accept == null ? requestBuilder : requestBuilder.header(HttpHeaders.ACCEPT, accept);
  }

  private Room createRoom() {
    final Room room = new Room();
    room.setId(ROOM_ID);
    room.setRevision("TestRev");
    room.setShortId("12345678");
    room.setName("Test Room");
    room.setOwnerId(OWNER_ID);
    room.setPassword(PASSWORD);
    return room;
  }
}

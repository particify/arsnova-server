package net.particify.arsnova.core.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.config.properties.AliasWordsProperties;
import net.particify.arsnova.core.model.RoomUserAlias;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.persistence.RoomUserAliasRepository;
import net.particify.arsnova.core.web.exceptions.BadRequestException;

@Service
@Primary
public class RoomUserAliasServiceImpl extends DefaultEntityServiceImpl<RoomUserAlias> implements RoomUserAliasService {
  private static final int NOUN_WEIGHTING = 3;
  private static final List<Character> L10N_GENDER = List.of('f', 'm', 'n');
  private static final Pattern choicePattern = Pattern.compile("(f|m|n)#");
  private final RoomUserAliasRepository roomUserAliasRepository;
  private final MessageSource messageSource;
  private final List<String> adjectives;
  private final List<String> nouns;
  private final List<String> specialNouns;
  private final AliasWordsProperties aliasWordsProperties;

  public RoomUserAliasServiceImpl(
      final RoomUserAliasRepository roomUserAliasRepository,
      final AliasWordsProperties aliasWordsProperties,
      @Qualifier("yamlMessageSource") final MessageSource messageSource,
      final DeletionRepository deletionRepository,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator) {
    super(
        RoomUserAlias.class,
        roomUserAliasRepository,
        deletionRepository,
        jackson2HttpMessageConverter.getObjectMapper(),
        validator);
    this.roomUserAliasRepository = roomUserAliasRepository;
    this.messageSource = messageSource;
    this.aliasWordsProperties = aliasWordsProperties;
    adjectives = List.copyOf(aliasWordsProperties.getAdjectives());
    nouns = List.copyOf(aliasWordsProperties.getNouns());
    specialNouns = List.copyOf(aliasWordsProperties.getSpecialNouns());
  }

  @Override
  public List<RoomUserAlias> getByRoomId(final String roomId) {
    return roomUserAliasRepository.findByRoomId(roomId);
  }

  @Override
  public RoomUserAlias getByRoomIdAndUserId(final String roomId, final String userId) {
    return roomUserAliasRepository.findByRoomIdAndUserId(roomId, userId);
  }

  @Override
  public List<RoomUserAlias> getByUserId(final String userId) {
    return roomUserAliasRepository.findByUserId(userId);
  }

  @Override
  public Map<String, RoomUserAlias> getUserAliasMappingsByRoomId(final String roomId, final Locale locale) {
    return getByRoomId(roomId).stream().collect(Collectors.toMap(
        a -> a.getUserId(),
        a -> a.getAlias() != null ? a : generateAlias(a, locale)
    ));
  }

  @Override
  protected void prepareCreate(final RoomUserAlias roomUserAlias) {
    super.prepareCreate(roomUserAlias);
    if (getByRoomIdAndUserId(roomUserAlias.getRoomId(), roomUserAlias.getUserId()) != null) {
      throw new BadRequestException("Alias has already been created.");
    }
    if (roomUserAlias.getSeed() == 0) {
      roomUserAlias.setSeed(ThreadLocalRandom.current().nextInt());
    }
  }

  private RoomUserAlias generateAlias(final RoomUserAlias roomUserAlias, final Locale locale) {
    final RoomUserAlias generatedAlias = generateAlias(roomUserAlias.getSeed(), locale);
    generatedAlias.setId(roomUserAlias.getId());
    return generatedAlias;
  }

  public RoomUserAlias generateAlias(final Locale locale) {
    return generateAlias(ThreadLocalRandom.current().nextInt(), locale);
  }

  @Override
  @SuppressFBWarnings("DMI_RANDOM_USED_ONLY_ONCE")
  public RoomUserAlias generateAlias(final int seed, final Locale locale) {
    final RoomUserAlias roomUserAlias = new RoomUserAlias();
    roomUserAlias.setSeed(seed);
    final Random seededRandom = new Random(seed);
    final String adjectiveKey = "alias-adjectives." + adjectives.get(seededRandom.nextInt(adjectives.size()));
    final int nounIndex = seededRandom.nextInt(nouns.size() * NOUN_WEIGHTING + specialNouns.size());
    final String nounKey = nounIndex > nouns.size() * NOUN_WEIGHTING
        ? "alias-special-nouns." + specialNouns.get(nounIndex % nouns.size())
        : "alias-nouns." + nouns.get(nounIndex % nouns.size());
    roomUserAlias.setAlias(localizeGeneratedAlias(adjectiveKey, nounKey, locale));
    return roomUserAlias;
  }

  private String localizeGeneratedAlias(final String adjectiveKey, final String nounKey, final Locale locale) {
    final String[] animal = messageSource.getMessage(nounKey, null, locale).split("\\|");
    final int genderIndex = animal.length > 1 ? L10N_GENDER.indexOf(animal[1].charAt(0)) : -1;
    final String adjectivePattern = choicePattern.matcher(messageSource.getMessage(adjectiveKey, null, locale))
        .replaceAll((matchResult -> L10N_GENDER.indexOf(matchResult.group(1).charAt(0)) + "#"));
    return MessageFormat.format(
        adjectivePattern,
        animal[0],
        genderIndex);
  }

  @Override
  public RoomUserAlias retrieveOrGenerateAlias(final String roomId, final String userId, final Locale locale) {
    final RoomUserAlias persistedAlias = getByRoomIdAndUserId(roomId, userId);
    if (persistedAlias != null) {
      final RoomUserAlias alias =
          persistedAlias.getAlias() != null ? persistedAlias : generateAlias(persistedAlias.getSeed(), locale);
      alias.setId(persistedAlias.getId());
      return alias;
    }
    return generateAlias(locale);
  }
}

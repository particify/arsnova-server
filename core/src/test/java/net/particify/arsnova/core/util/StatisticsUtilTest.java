package net.particify.arsnova.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
public class StatisticsUtilTest {
  private static final List<Double> SOME_NUMBERS = Arrays.asList(38.0, 40.0, 42.0, 47.0, 43.0, 42.0, 41.0, 99.0);

  @Test
  public void testMeanCalculationIsCorrect() {
    final Double mean = StatisticsUtil.calculateMean(SOME_NUMBERS);
    assertNotNull(mean);
    assertEquals(mean, 49.0);
    final Double meanFromEmpty = StatisticsUtil.calculateMean(Collections.emptyList());
    assertNotNull(meanFromEmpty);
    assertEquals(meanFromEmpty, 0.0);
  }

  @Test
  public void testMedianCalculationIsCorrect() {
    final Double median = StatisticsUtil.calculateMedian(SOME_NUMBERS);
    assertNotNull(median);
    assertEquals(median, 42.0);
    final Double medianFromEmpty = StatisticsUtil.calculateMedian(Collections.emptyList());
    assertNotNull(medianFromEmpty);
    assertEquals(medianFromEmpty, 0.0);
  }

  @Test
  public void testStandardDeviationCalculationIsCorrect() {
    final Double standardDeviation = StatisticsUtil.calculateStandardDeviation(SOME_NUMBERS);
    assertNotNull(standardDeviation);
    assertEquals(standardDeviation, 19.05255888325765);
    final Double standardDeviationFromEmpty = StatisticsUtil.calculateStandardDeviation(Collections.emptyList());
    assertNotNull(standardDeviationFromEmpty);
    assertEquals(standardDeviationFromEmpty, 0.0);
  }

  @Test
  public void testVarianceCalculationIsCorrect() {
    final Double variance = StatisticsUtil.calculateVariance(SOME_NUMBERS);
    assertNotNull(variance);
    assertEquals(variance, 363.0);
    final Double varianceFromEmpty = StatisticsUtil.calculateVariance(Collections.emptyList());
    assertNotNull(varianceFromEmpty);
    assertEquals(varianceFromEmpty, 0.0);
  }

  @Test
  public void testFindingMinimumIsCorrect() {
    final Double minimum = StatisticsUtil.findMinimum(SOME_NUMBERS);
    assertNotNull(minimum);
    assertEquals(minimum, 38.0);
    final Double minimumFromEmpty = StatisticsUtil.findMinimum(Collections.emptyList());
    assertNotNull(minimumFromEmpty);
    assertEquals(minimumFromEmpty, 0.0);
  }

  @Test
  public void testFindingMaximumIsCorrect() {
    final Double maximum = StatisticsUtil.findMaximum(SOME_NUMBERS);
    assertNotNull(maximum);
    assertEquals(maximum, 99.0);
    final Double maximumFromEmpty = StatisticsUtil.findMaximum(Collections.emptyList());
    assertNotNull(maximumFromEmpty);
    assertEquals(maximumFromEmpty, 0.0);
  }
}

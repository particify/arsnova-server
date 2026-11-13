package net.particify.arsnova.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;

public class RabbitConfigTest {
  @Test
  public void testShouldDeliverMessageAsOftenAsConfiguredMaxAttempts() throws Throwable {
    final AtomicInteger deliveries = new AtomicInteger();
    final MethodInvocation failingListener = mock(MethodInvocation.class);
    when(failingListener.getArguments()).thenReturn(new Object[] {null, new Message(new byte[0])});
    when(failingListener.proceed()).thenAnswer(invocation -> {
      deliveries.incrementAndGet();
      throw new IllegalStateException("Listener failure");
    });

    assertThrows(
        ListenerExecutionFailedException.class,
        () -> new RabbitConfig().retryInterceptor(3).invoke(failingListener));
    assertEquals(3, deliveries.get());
  }
}

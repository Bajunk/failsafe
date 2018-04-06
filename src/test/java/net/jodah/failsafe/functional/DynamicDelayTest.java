/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package net.jodah.failsafe.functional;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.util.Duration;

@Test
public class DynamicDelayTest {
  static class UncheckedExpectedException extends RuntimeException {
  }

  static class DelayException extends UncheckedExpectedException {
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullDelayFunction() {
    new RetryPolicy().withDelay(null);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullResult() {
    new RetryPolicy().withDelayWhen((result, failure, context) -> new Duration(1L, TimeUnit.SECONDS), null);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullFailureType() {
    new RetryPolicy().withDelayOn((result, failure, context) -> new Duration(1L, TimeUnit.SECONDS), null);
  }

  @Test(expectedExceptions = UncheckedExpectedException.class)
  public void testUncheckedExceptionInDelayFunction() {
    RetryPolicy retryPolicy = new RetryPolicy().withDelay((result, failure, context) -> {
      throw new UncheckedExpectedException();
    });

    Failsafe.with(retryPolicy).run((ExecutionContext context) -> {
      throw new RuntimeException("try again");
    });
  }

  public void shouldDelayOnMatchingResult() {
    AtomicInteger delays = new AtomicInteger(0);
    RetryPolicy retryPolicy = new RetryPolicy().retryIf(result -> true).withMaxRetries(4).withDelayWhen((r, f, c) -> {
      delays.incrementAndGet(); // side-effect for test purposes
      return new Duration(1L, TimeUnit.MICROSECONDS);
    }, "expected");

    AtomicInteger attempts = new AtomicInteger(0);
    Object result = Failsafe.with(retryPolicy).withFallback(123).get(() -> {
      int i = attempts.getAndIncrement();
      switch (i) {
        case 0:
        case 3:
          return "expected";
        default:
          return i;
      }
    });

    assertEquals(result, 123, "Fallback should be used");
    assertEquals(attempts.get(), 5, "Expecting five attempts (1 + 4 retries)");
    assertEquals(delays.get(), 2, "Expecting two dynamic delays matching String result");
  }

  public void shouldDelayOnMatchingFailureType() {
    AtomicInteger delays = new AtomicInteger(0);
    RetryPolicy retryPolicy = new RetryPolicy()
        .retryOn(UncheckedExpectedException.class)
        .withMaxRetries(4)
        .withDelayOn((r, f, c) -> {
          delays.incrementAndGet(); // side-effect for test purposes
          return new Duration(1L, TimeUnit.MICROSECONDS);
        }, DelayException.class);

    AtomicInteger attempts = new AtomicInteger(0);
    int result = Failsafe.with(retryPolicy).withFallback(123).get(() -> {
      int i = attempts.getAndIncrement();
      switch (i) {
        case 0:
        case 2:
          throw new DelayException();
        default:
          throw new UncheckedExpectedException();
      }
    });

    assertEquals(result, 123, "Fallback should be used");
    assertEquals(attempts.get(), 5, "Expecting five attempts (1 + 4 retries)");
    assertEquals(delays.get(), 2, "Expecting two dynamic delays matching DelayException failure");
  }
}
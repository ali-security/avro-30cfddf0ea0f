/*
 * Copyright 2017 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.avro;

import static org.apache.avro.SystemLimitException.*;

import org.junit.Assert;
import org.junit.Test;
import org.junit.After;

import java.util.function.Function;
import java.util.concurrent.Callable;

public class TestSystemLimitException {

  /** Delegated here for package visibility. */
  public static final int MAX_ARRAY_VM_LIMIT = SystemLimitException.MAX_ARRAY_VM_LIMIT;

  public static final String ERROR_NEGATIVE = "Malformed data. Length is negative: -1";
  public static final String ERROR_VM_LIMIT_BYTES = "Cannot read arrays longer than " + MAX_ARRAY_VM_LIMIT
      + " bytes in Java library";
  public static final String ERROR_VM_LIMIT_COLLECTION = "Cannot read collections larger than " + MAX_ARRAY_VM_LIMIT
      + " items in Java library";
  public static final String ERROR_VM_LIMIT_STRING = "Cannot read strings longer than " + MAX_ARRAY_VM_LIMIT + " bytes";

  /** Delegated here for package visibility. */
  public static void resetLimits() {
    SystemLimitException.resetLimits();
  }

  @After
  public void reset() {
    System.clearProperty(MAX_BYTES_LENGTH_PROPERTY);
    System.clearProperty(MAX_COLLECTION_LENGTH_PROPERTY);
    System.clearProperty(MAX_STRING_LENGTH_PROPERTY);
    resetLimits();
  }

  /**
   * A helper method that tests the consistent limit handling from system
   * properties.
   *
   * @param f                The function to be tested.
   * @param sysProperty      The system property used to control the custom limit.
   * @param errorVmLimit     The error message used when the property would be
   *                         over the VM limit.
   * @param errorCustomLimit The error message used when the property would be
   *                         over the custom limit of 1000.
   */
  public void helpCheckSystemLimits(Function<Long, Integer> f, String sysProperty, String errorVmLimit,
      String errorCustomLimit) {
    // Correct values pass through
    Assert.assertEquals("correct", (Integer) 0, (Integer) f.apply(0L));
    Assert.assertEquals(1024L, (long) f.apply(1024L));
    Assert.assertEquals((Integer) MAX_ARRAY_VM_LIMIT, (Integer) f.apply((long) MAX_ARRAY_VM_LIMIT));

    // Values that exceed the default system limits throw exceptions
    Exception ex = assertThrows(UnsupportedOperationException.class, () -> {
      f.apply(Long.MAX_VALUE);
      return null;
    });
    Assert.assertEquals(errorVmLimit, ex.getMessage());
    ex = assertThrows(UnsupportedOperationException.class, () -> f.apply((long) MAX_ARRAY_VM_LIMIT + 1));
    Assert.assertEquals(errorVmLimit, ex.getMessage());
    ex = assertThrows(AvroRuntimeException.class, () -> f.apply(-1L));
    Assert.assertEquals(ERROR_NEGATIVE, ex.getMessage());

    // Setting the system property to provide a custom limit.
    System.setProperty(sysProperty, Long.toString(1000L));
    resetLimits();

    // Correct values pass through
    Assert.assertEquals((Integer) 0, f.apply(0L));
    Assert.assertEquals((Integer) 102, f.apply(102L));

    // Values that exceed the custom system limits throw exceptions
    ex = assertThrows(UnsupportedOperationException.class, () -> f.apply((long) MAX_ARRAY_VM_LIMIT + 1));
    Assert.assertEquals(errorVmLimit, ex.getMessage());
    ex = assertThrows(SystemLimitException.class, () -> f.apply(1024L));
    Assert.assertEquals(errorCustomLimit, ex.getMessage());
    ex = assertThrows(AvroRuntimeException.class, () -> f.apply(-1L));
    Assert.assertEquals(ERROR_NEGATIVE, ex.getMessage());
  }

  @Test
  public void testCheckMaxBytesLength() {
    helpCheckSystemLimits(SystemLimitException::checkMaxBytesLength, MAX_BYTES_LENGTH_PROPERTY, ERROR_VM_LIMIT_BYTES,
        "Bytes length 1024 exceeds maximum allowed");
  }

  @Test
  public void testCheckMaxCollectionLengthFromZero() {
    helpCheckSystemLimits(l -> checkMaxCollectionLength(0L, l), MAX_COLLECTION_LENGTH_PROPERTY,
        ERROR_VM_LIMIT_COLLECTION, "Collection length 1024 exceeds maximum allowed");
  }

  @Test
  public void testCheckMaxStringLength() {
    helpCheckSystemLimits(SystemLimitException::checkMaxStringLength, MAX_STRING_LENGTH_PROPERTY, ERROR_VM_LIMIT_STRING,
        "String length 1024 exceeds maximum allowed");
  }

  @Test
  public void testCheckMaxCollectionLengthFromNonZero() {
    // Correct values pass through
    Assert.assertEquals(10, checkMaxCollectionLength(10L, 0L));
    Assert.assertEquals(MAX_ARRAY_VM_LIMIT, checkMaxCollectionLength(10L, MAX_ARRAY_VM_LIMIT - 10L));
    Assert.assertEquals(MAX_ARRAY_VM_LIMIT, checkMaxCollectionLength(MAX_ARRAY_VM_LIMIT - 10L, 10L));

    // Values that exceed the default system limits throw exceptions
    Exception ex = assertThrows(UnsupportedOperationException.class,
        () -> checkMaxCollectionLength(10L, MAX_ARRAY_VM_LIMIT - 9L));
    Assert.assertEquals(ERROR_VM_LIMIT_COLLECTION, ex.getMessage());
    ex = assertThrows(UnsupportedOperationException.class,
        () -> checkMaxCollectionLength(SystemLimitException.MAX_ARRAY_VM_LIMIT - 9L, 10L));
    Assert.assertEquals(ERROR_VM_LIMIT_COLLECTION, ex.getMessage());

    ex = assertThrows(UnsupportedOperationException.class, () -> checkMaxCollectionLength(10L, Long.MAX_VALUE - 10L));
    Assert.assertEquals(ERROR_VM_LIMIT_COLLECTION, ex.getMessage());
    ex = assertThrows(UnsupportedOperationException.class, () -> checkMaxCollectionLength(Long.MAX_VALUE - 10L, 10L));
    Assert.assertEquals(ERROR_VM_LIMIT_COLLECTION, ex.getMessage());

    // Overflow that adds to negative
    ex = assertThrows(UnsupportedOperationException.class, () -> checkMaxCollectionLength(10L, Long.MAX_VALUE));
    Assert.assertEquals(ERROR_VM_LIMIT_COLLECTION, ex.getMessage());
    ex = assertThrows(UnsupportedOperationException.class, () -> checkMaxCollectionLength(Long.MAX_VALUE, 10L));
    Assert.assertEquals(ERROR_VM_LIMIT_COLLECTION, ex.getMessage());

    ex = assertThrows(AvroRuntimeException.class, () -> checkMaxCollectionLength(10L, -1L));
    Assert.assertEquals(ERROR_NEGATIVE, ex.getMessage());
    ex = assertThrows(AvroRuntimeException.class, () -> checkMaxCollectionLength(-1L, 10L));
    Assert.assertEquals(ERROR_NEGATIVE, ex.getMessage());

    // Setting the system property to provide a custom limit.
    System.setProperty(MAX_COLLECTION_LENGTH_PROPERTY, Long.toString(1000L));
    resetLimits();

    // Correct values pass through
    Assert.assertEquals(10, checkMaxCollectionLength(10L, 0L));
    Assert.assertEquals(102, checkMaxCollectionLength(10L, 92L));
    Assert.assertEquals(102, checkMaxCollectionLength(92L, 10L));

    // Values that exceed the custom system limits throw exceptions
    ex = assertThrows(UnsupportedOperationException.class, () -> checkMaxCollectionLength(MAX_ARRAY_VM_LIMIT, 1));
    Assert.assertEquals(ERROR_VM_LIMIT_COLLECTION, ex.getMessage());
    ex = assertThrows(UnsupportedOperationException.class, () -> checkMaxCollectionLength(1, MAX_ARRAY_VM_LIMIT));
    Assert.assertEquals(ERROR_VM_LIMIT_COLLECTION, ex.getMessage());

    ex = assertThrows(SystemLimitException.class, () -> checkMaxCollectionLength(999, 25));
    Assert.assertEquals("Collection length 1024 exceeds maximum allowed", ex.getMessage());
    ex = assertThrows(SystemLimitException.class, () -> {
      checkMaxCollectionLength(25, 999);
      return null;
    });
    Assert.assertEquals("Collection length 1024 exceeds maximum allowed", ex.getMessage());
  }

  /**
   * A convenience method to avoid a large number of @Test(expected=...) tests
   *
   * @param expected An Exception class that the Runnable should throw
   * @param callable A Callable that is expected to throw the exception
   *
   * @return The exception that was thrown
   */
  public static Exception assertThrows(Class<? extends Exception> expected, Callable callable) {
    try {
      callable.call();
      Assert.fail("No exception was thrown, expected: " + expected.getName());
    } catch (Exception actual) {
      Assert.assertEquals(expected, actual.getClass());
      return actual;
    }
    return null;
  }
}

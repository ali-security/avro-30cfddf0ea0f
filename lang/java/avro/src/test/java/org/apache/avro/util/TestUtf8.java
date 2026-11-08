/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.avro.util;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;

import org.apache.avro.SystemLimitException;
import org.apache.avro.TestSystemLimitException;
import org.junit.Test;

public class TestUtf8 {
  @Test
  public void oversizeUtf8() {
    Utf8 u = new Utf8();
    u.setByteLength(1024);
    assertEquals(1024, u.getByteLength());
    assertThrows(UnsupportedOperationException.class,
        () -> u.setByteLength(TestSystemLimitException.MAX_ARRAY_VM_LIMIT + 1));

    try {
      System.setProperty(SystemLimitException.MAX_STRING_LENGTH_PROPERTY, Long.toString(1000L));
      TestSystemLimitException.resetLimits();

      Exception ex = assertThrows(SystemLimitException.class, () -> u.setByteLength(1024));
      assertEquals("String length 1024 exceeds maximum allowed", ex.getMessage());
    } finally {
      System.clearProperty(SystemLimitException.MAX_STRING_LENGTH_PROPERTY);
      TestSystemLimitException.resetLimits();
    }
  }

  @Test
  public void testByteConstructor() throws Exception {
    byte[] bs = "Foo".getBytes(StandardCharsets.UTF_8);
    Utf8 u = new Utf8(bs);
    assertEquals(bs.length, u.getByteLength());
    for (int i = 0; i < bs.length; i++) {
      assertEquals(bs[i], u.getBytes()[i]);
    }
  }

  @Test
  public void testArrayReusedWhenLargerThanRequestedSize() {
    byte[] bs = "55555".getBytes(StandardCharsets.UTF_8);
    Utf8 u = new Utf8(bs);
    assertEquals(5, u.getByteLength());
    byte[] content = u.getBytes();
    u.setByteLength(3);
    assertEquals(3, u.getByteLength());
    assertSame(content, u.getBytes());
    u.setByteLength(4);
    assertEquals(4, u.getByteLength());
    assertSame(content, u.getBytes());
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
      fail("No exception was thrown, expected: " + expected.getName());
    } catch (Exception actual) {
      assertEquals(expected, actual.getClass());
      return actual;
    }
    return null;
  }
}

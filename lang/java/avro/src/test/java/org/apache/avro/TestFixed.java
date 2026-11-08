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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.apache.avro;

import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Test;

public class TestFixed {
  @Test
  public void fixedLengthOutOfLimit() {
    Exception ex = assertThrows(UnsupportedOperationException.class,
        () -> Schema.createFixed("oversize", "doc", "space", Integer.MAX_VALUE));
    Assert.assertEquals(TestSystemLimitException.ERROR_VM_LIMIT_BYTES, ex.getMessage());
  }

  @Test
  public void fixedNegativeLength() {
    Exception ex = assertThrows(AvroRuntimeException.class, () -> Schema.createFixed("negative", "doc", "space", -1));
    Assert.assertEquals(TestSystemLimitException.ERROR_NEGATIVE, ex.getMessage());
  }

  @Test
  public void testFixedDefaultValueDrop() {
    Schema md5 = SchemaBuilder.builder().fixed("MD5").size(16);
    Schema frec = SchemaBuilder.builder().record("test").fields().name("hash").type(md5).withDefault(new byte[16])
        .endRecord();
    Schema.Field field = frec.getField("hash");
    Assert.assertNotNull(field.defaultVal());
    Assert.assertArrayEquals(new byte[16], (byte[]) field.defaultVal());
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

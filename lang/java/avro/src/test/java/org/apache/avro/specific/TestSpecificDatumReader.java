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
package org.apache.avro.specific;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Checks that {@link SpecificDatumReader} refuses to instantiate untrusted
 * classes named by the java-class and java-key-class schema properties.
 */
public class TestSpecificDatumReader {

  private static final String DENIED_CLASS = "javax.management.BadAttributeValueExpException";

  private static final Schema STRING_WRITER = Schema.create(Schema.Type.STRING);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testDeniedClassWithDefaultConstructor() throws IOException {
    assertForbidden(new SpecificDatumReader<Object>(), DENIED_CLASS);
  }

  @Test
  public void testDeniedClassWithSpecificDataConstructor() throws IOException {
    assertForbidden(new SpecificDatumReader<Object>(SpecificData.get()), DENIED_CLASS);
  }

  @Test
  public void testDeniedClassWithoutFastReader() throws IOException {
    SpecificData data = new SpecificData();
    data.setFastReaderEnabled(false);
    assertForbidden(new SpecificDatumReader<Object>(data), DENIED_CLASS);
  }

  @Test
  public void testDeniedClassWithFastReader() throws IOException {
    SpecificData data = new SpecificData();
    data.setFastReaderEnabled(true);
    assertForbidden(new SpecificDatumReader<Object>(data), DENIED_CLASS);
  }

  @Test
  public void testDeniedJdkClassesMatchedByFullName() throws IOException {
    for (String className : Arrays.asList("java.net.URLClassLoader", "javax.script.ScriptEngineManager",
        "java.rmi.server.UnicastRemoteObject", DENIED_CLASS)) {
      for (SpecificDatumReader<Object> reader : newReaders()) {
        assertForbidden(reader, className);
      }
    }
  }

  @Test
  public void testDeniedMapKeyClass() throws IOException {
    Schema writer = Schema.createMap(Schema.create(Schema.Type.STRING));
    for (SpecificDatumReader<Object> reader : newReaders()) {
      Schema expected = mapSchema(DENIED_CLASS);
      assertForbidden(reader, writer, expected, Collections.singletonMap("key", "value"), DENIED_CLASS);
    }
  }

  @Test
  public void testDeniedClassNestedInRecord() throws IOException {
    Schema writer = recordSchema(STRING_WRITER);
    GenericData.Record record = new GenericData.Record(writer);
    record.put("f", Collections.singletonList("value"));
    for (SpecificDatumReader<Object> reader : newReaders()) {
      assertForbidden(reader, writer, recordSchema(stringSchema(DENIED_CLASS)), record, DENIED_CLASS);
    }
  }

  @Test
  public void testPackageEntryBlocksClassesInPackage() throws IOException {
    // java.io.FileOutputStream has a String constructor that creates the file
    // named by the datum: an untrusted package entry must stop it before that.
    int i = 0;
    for (SpecificDatumReader<Object> reader : newReaders()) {
      reader.getUntrustedPackages().add("java.io");
      File target = new File(temp.getRoot(), "created-by-java-class-" + i++);
      assertForbidden(reader, STRING_WRITER, stringSchema("java.io.FileOutputStream"), target.getPath(),
          "java.io.FileOutputStream");
      assertFalse("java-class instantiated " + target, target.exists());
    }
  }

  @Test
  public void testPackageEntryBlocksClassesInSubPackages() throws IOException {
    for (SpecificDatumReader<Object> reader : newReaders()) {
      reader.getUntrustedPackages().add("java.util");
      assertForbidden(reader, "java.util.jar.JarFile");
    }
  }

  @Test
  public void testPackageEntryMatchesOnlyWholePackageNames() throws IOException {
    for (SpecificDatumReader<Object> reader : newReaders()) {
      reader.getUntrustedPackages().add("java.math.Big");
      reader.getUntrustedPackages().add("java.mat");
      Object datum = read(reader, STRING_WRITER, stringSchema("java.math.BigDecimal"), "12.34");
      assertEquals(new BigDecimal("12.34"), datum);
    }
  }

  @Test
  public void testTrustedClassStillDeserializes() throws IOException {
    for (SpecificDatumReader<Object> reader : newReaders()) {
      Object datum = read(reader, STRING_WRITER, stringSchema("java.math.BigDecimal"), "12.34");
      assertEquals(new BigDecimal("12.34"), datum);
    }
  }

  @Test
  public void testTrustedMapKeyClassStillDeserializes() throws IOException {
    Schema writer = Schema.createMap(Schema.create(Schema.Type.STRING));
    for (SpecificDatumReader<Object> reader : newReaders()) {
      Schema expected = mapSchema("java.math.BigDecimal");
      Map<?, ?> map = (Map<?, ?>) read(reader, writer, expected, Collections.singletonMap("1.5", "value"));
      assertEquals(Collections.singleton(new BigDecimal("1.5")), map.keySet());
    }
  }

  @Test
  public void testUntrustedPackagesInitializedByEveryConstructor() {
    Schema schema = TestRecordWithLogicalTypes.SCHEMA$;
    List<SpecificDatumReader<?>> readers = Arrays.<SpecificDatumReader<?>>asList(
        new SpecificDatumReader<Object>(),
        new SpecificDatumReader<TestRecordWithLogicalTypes>(TestRecordWithLogicalTypes.class),
        new SpecificDatumReader<Object>(schema), new SpecificDatumReader<Object>(schema, schema),
        new SpecificDatumReader<Object>(schema, schema, new SpecificData()),
        new SpecificDatumReader<Object>(new SpecificData()));
    for (SpecificDatumReader<?> reader : readers) {
      assertEquals(Arrays.asList(SpecificDatumReader.DENY_LIST), reader.getUntrustedPackages());
      assertTrue(reader.getUntrustedPackages().contains("java.net.URLClassLoader"));
      assertFalse(reader.getUntrustedPackages().contains("URLClassLoader"));
    }
  }

  /**
   * Readers for every way of reaching a {@link SpecificDatumReader}: both
   * constructors, with the fast reader both disabled and enabled.
   */
  private static List<SpecificDatumReader<Object>> newReaders() {
    SpecificData slow = new SpecificData();
    slow.setFastReaderEnabled(false);
    SpecificData fast = new SpecificData();
    fast.setFastReaderEnabled(true);
    return Arrays.asList(new SpecificDatumReader<Object>(), new SpecificDatumReader<Object>(SpecificData.get()),
        new SpecificDatumReader<Object>(slow), new SpecificDatumReader<Object>(fast));
  }

  /**
   * A string schema naming a java-class. It is read as a java.lang.String, which
   * the fast reader needs before it passes the value to the class constructor.
   */
  private static Schema stringSchema(String javaClass) {
    Schema schema = Schema.create(Schema.Type.STRING);
    schema.addProp(GenericData.STRING_PROP, GenericData.StringType.String.name());
    schema.addProp(SpecificData.CLASS_PROP, javaClass);
    return schema;
  }

  /** A map schema naming a java-key-class, with keys read as java.lang.String. */
  private static Schema mapSchema(String javaKeyClass) {
    Schema schema = Schema.createMap(Schema.create(Schema.Type.STRING));
    schema.addProp(GenericData.STRING_PROP, GenericData.StringType.String.name());
    schema.addProp(SpecificData.KEY_CLASS_PROP, javaKeyClass);
    return schema;
  }

  /** A record whose only field is an array of a union of null and the given type. */
  private static Schema recordSchema(Schema stringType) {
    Schema items = Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), stringType));
    Schema.Field field = new Schema.Field("f", Schema.createArray(items), null, (Object) null);
    Schema record = Schema.createRecord("TestSpecificDatumReaderRecord", null, "org.apache.avro.specific.test",
        false);
    record.setFields(Collections.singletonList(field));
    return record;
  }

  private static void assertForbidden(SpecificDatumReader<Object> reader, String className) throws IOException {
    assertForbidden(reader, STRING_WRITER, stringSchema(className), "value", className);
  }

  private static void assertForbidden(SpecificDatumReader<Object> reader, Schema writer, Schema expected, Object datum,
      String className) throws IOException {
    try {
      Object result = read(reader, writer, expected, datum);
      fail("Expected SecurityException for " + className + " but read " + result);
    } catch (SecurityException e) {
      assertTrue(e.getMessage(), e.getMessage().contains(className));
    }
  }

  private static Object read(SpecificDatumReader<Object> reader, Schema writer, Schema expected, Object datum)
      throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
    new GenericDatumWriter<Object>(writer).write(datum, encoder);
    encoder.flush();

    reader.setExpected(expected);
    reader.setSchema(writer);
    Decoder decoder = DecoderFactory.get().binaryDecoder(out.toByteArray(), null);
    return reader.read(null, decoder);
  }
}

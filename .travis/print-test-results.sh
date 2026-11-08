#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# The suite runs with surefire parallel=all, so the console shows only per-module
# totals and a "Skipped: N" count. Print per-class results and every skipped test
# case (class, name and skip reason) from the surefire XML reports of the last run.
echo "===== Per-class test results (from surefire XML reports) ====="
find "${1:-.}" -path '*/target/surefire-reports/*' -name 'TEST-*.xml' -print0 | sort -z \
  | xargs -0 -r awk '
      /<testsuite / {
        n = $0; sub(/.*<testsuite[^>]* name="/, "", n); sub(/".*/, "", n)
        t = $0; sub(/.*[ ]tests="/, "", t); sub(/".*/, "", t)
        f = $0; sub(/.*[ ]failures="/, "", f); sub(/".*/, "", f)
        e = $0; sub(/.*[ ]errors="/, "", e); sub(/".*/, "", e)
        k = $0; sub(/.*[ ]skipped="/, "", k); sub(/".*/, "", k)
        printf "Tests run: %s, Failures: %s, Errors: %s, Skipped: %s - in %s\n", t, f, e, k, n
        nextfile
      }'
echo "===== Skipped tests (from surefire XML reports) ====="
find "${1:-.}" -path '*/target/surefire-reports/*' -name 'TEST-*.xml' -print0 \
  | xargs -0 -r awk '
      /<testcase / {
        tc = $0
        name = tc; sub(/.*<testcase name="/, "", name); sub(/".*/, "", name)
        cls = tc; sub(/.*classname="/, "", cls); sub(/".*/, "", cls)
      }
      /<skipped\/>/ {
        print "SKIPPED " cls "." name " -- reason: annotated @Ignore in the upstream test source (JUnit @Ignore without a message)"
        next
      }
      /<skipped / {
        msg = $0; sub(/.*message="/, "", msg); sub(/".*/, "", msg)
        print "SKIPPED " cls "." name " -- reason: JUnit Assume not satisfied (" msg ")"
      }'
cat <<'NOTES'
Notes on the skips above (all present in the unmodified upstream test sources):
 - TestSpecificRecordBuilder.testBuilderPerformance / testBuilderPerformanceWithDefaultValues /
   testManualBuildPerformance and TestNettyServerWithCallbacks.performanceTest are throughput
   micro-benchmarks that upstream keeps @Ignore'd; they assert nothing about behaviour.
 - TestReflectLogicalTypes.testReadUUIDMissingLogicalTypeReflect calls
   Assume.assumeTrue(ReflectionUtil.getFieldAccess() instanceof FieldAccessReflect): only one
   FieldAccess implementation can be chosen per JVM, and the forked test JVM has already
   initialised the Unsafe-based one, so JUnit skips it by design.
NOTES

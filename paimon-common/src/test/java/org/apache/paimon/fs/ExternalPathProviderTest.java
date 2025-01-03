/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.fs;

import org.apache.paimon.CoreOptions.ExternalFSStrategy;
import org.apache.paimon.CoreOptions.ExternalPathStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ExternalPathProviderTest {

    private ExternalPathProvider provider;

    @BeforeEach
    public void setUp() {
        provider =
                new ExternalPathProvider(
                        "oss://bucket1/path1,s3://bucket2/path2",
                        ExternalPathStrategy.ROUND_ROBIN,
                        null,
                        "db/table");
    }

    @Test
    public void testInitExternalPaths() {
        assertTrue(provider.externalPathExists());
        assertEquals(2, provider.getExternalPathsMap().size());
        assertEquals(2, provider.getExternalPathsList().size());
    }

    @Test
    public void testGetNextExternalPathRoundRobin() {
        Optional<Path> path1 = provider.getNextExternalPath();
        assertTrue(path1.isPresent());
        assertEquals("s3://bucket2/path2/db/table", path1.get().toString());

        Optional<Path> path2 = provider.getNextExternalPath();
        assertTrue(path2.isPresent());
        assertEquals("oss://bucket1/path1/db/table", path2.get().toString());

        Optional<Path> path3 = provider.getNextExternalPath();
        assertTrue(path3.isPresent());
        assertEquals("s3://bucket2/path2/db/table", path3.get().toString());
    }

    @Test
    public void testGetNextExternalPathSpecificFS() {
        provider =
                new ExternalPathProvider(
                        "oss://bucket1/path1,s3://bucket2/path2",
                        ExternalPathStrategy.SPECIFIC_FS,
                        ExternalFSStrategy.OSS,
                        "db/table");

        Optional<Path> path = provider.getNextExternalPath();
        assertTrue(path.isPresent());
        assertEquals("oss://bucket1/path1/db/table", path.get().toString());
    }

    @Test
    public void testGetNextExternalPathNone() {
        provider =
                new ExternalPathProvider(
                        "oss://bucket1/path1,s3://bucket2/path2",
                        ExternalPathStrategy.NONE,
                        ExternalFSStrategy.OSS,
                        "db/table");

        Optional<Path> path = provider.getNextExternalPath();
        assertFalse(path.isPresent());
    }

    @Test
    public void testUnsupportedExternalPath() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new ExternalPathProvider(
                            "hdfs://bucket1/path1",
                            ExternalPathStrategy.ROUND_ROBIN,
                            ExternalFSStrategy.OSS,
                            "db/table");
                });
    }

    @Test
    public void testUnsupportedExternalFSStrategy() {
        provider =
                new ExternalPathProvider(
                        "oss://bucket1/path1",
                        ExternalPathStrategy.SPECIFIC_FS,
                        ExternalFSStrategy.S3,
                        "db/table");
        Optional<Path> path = provider.getNextExternalPath();
        assertFalse(path.isPresent());
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sease.rre.search.api.impl;

import io.sease.rre.search.api.SearchPlatform;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApacheSolrTest {

    private static final String INDEX_NAME = "test";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private SearchPlatform platform;

    @Before
    public void setupPlatform() {
        platform = new ApacheSolr();
    }

    @Test
    public void isSearchPlatformFile_returnsFalseWhenNotDirectory() throws Exception {
        File dummyFile = tempFolder.newFile();
        assertFalse(platform.isSearchPlatformFile(INDEX_NAME, dummyFile));
    }

    @Test
    public void isSearchPlatformFile_returnsFalseWhenDirectoryNotContainSolrConfig() {
        assertFalse(platform.isSearchPlatformFile(INDEX_NAME, tempFolder.getRoot()));
    }

    @Test
    public void isSearchPlatformFile_returnsTrueWhenDirectoryContainsSolrConfig() throws Exception {
        File configFile = tempFolder.newFolder(INDEX_NAME);
        assertTrue(platform.isSearchPlatformFile(INDEX_NAME, configFile));
    }
}

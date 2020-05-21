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
package io.sease.rre.core.template.impl;

import io.sease.rre.core.template.QueryTemplateManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the {@link QueryTemplateManager} that will cache the
 * template contents.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
public class CachingQueryTemplateManager extends FileQueryTemplateManager implements QueryTemplateManager {

    private final Map<File, String> templatePathMap = new HashMap<>();

    /**
     * Initialise the query template manager with template folder path.
     *
     * @param templatesFolderPath the path to the template folder.
     * @throws IllegalArgumentException if the folder path doesn't point to a
     *                                  directory, or the directory cannot be read.
     */
    public CachingQueryTemplateManager(String templatesFolderPath) throws IllegalArgumentException {
        this(new File(templatesFolderPath));
    }

    /**
     * Initialise the query template manager with a template folder.
     *
     * @param templatesFolder the template folder.
     * @throws IllegalArgumentException if the given folder isn't a directory,
     *                                  or the directory cannot be read.
     */
    public CachingQueryTemplateManager(File templatesFolder) throws IllegalArgumentException {
        super(templatesFolder);
    }

    @Override
    public String getTemplate(final String defaultTemplate, final String template, final String version) throws IOException {
        File templateFile = getTemplateFile(defaultTemplate, template, version);
        templatePathMap.putIfAbsent(templateFile, readTemplateContent(templateFile));

        return templatePathMap.get(templateFile);
    }
}

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

/**
 * Basic implementation of the QueryTemplateManager interface.
 *
 * @author Matt Pearce (matt@elysiansoftware.co.uk)
 */
public class FileQueryTemplateManager implements QueryTemplateManager {

    private final File templatesFolder;

    /**
     * Initialise the query template manager with a template folder.
     *
     * @param templatesFolder the template folder.
     * @throws IllegalArgumentException if the given folder isn't a directory,
     *                                  or the directory cannot be read.
     */
    public FileQueryTemplateManager(File templatesFolder) {
        this.templatesFolder = templatesFolder;
        if (!templatesFolder.isDirectory() || !templatesFolder.canRead()) {
            throw new IllegalArgumentException("Unable to read from query template directory " + templatesFolder.getAbsolutePath());
        }
    }

    @Override
    public String getTemplate(String defaultTemplate, String template, String version) throws FileNotFoundException, IOException {
        return readTemplateContent(getTemplateFile(defaultTemplate, template, version));
    }

    protected File getTemplateFile(String defaultTemplate, String template, String version) {
        String templateName = Optional.ofNullable(getTemplate(defaultTemplate, template))
                .orElseThrow(() -> new IllegalArgumentException("No template name supplied!"));
        return buildTemplatePath(templateName, version);
    }

    protected String getTemplate(String defaultTemplate, String template) {
        return template == null ? defaultTemplate : template;
    }

    protected File buildTemplatePath(String template, String version) {
        final File templateFile;

        if (template.contains(VERSION_PLACEHOLDER)) {
            templateFile = new File(getVersionedTemplateFolder(version), template.replace(VERSION_PLACEHOLDER, version));
        } else {
            templateFile = new File(getVersionedTemplateFolder(version), template);
        }

        return templateFile;
    }

    protected File getVersionedTemplateFolder(String version) {
        File versionedFolder = new File(templatesFolder, version);
        return (versionedFolder.canRead() && versionedFolder.isDirectory()) ? versionedFolder : templatesFolder;
    }

    /**
     * Reads a template content.
     *
     * @param file the template file.
     * @return the template content.
     * @throws IOException      if the file cannot be read.
     * @throws RuntimeException if other exceptions are thrown (OutOfMemory, SecurityException).
     */
    protected String readTemplateContent(final File file) throws IOException {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (final IOException e) {
            throw e;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}

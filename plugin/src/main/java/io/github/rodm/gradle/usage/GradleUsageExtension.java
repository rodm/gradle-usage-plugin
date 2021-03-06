/*
 * Copyright 2022 Rod MacKenzie.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.rodm.gradle.usage;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

public class GradleUsageExtension {

    private final ListProperty<String> paths;
    private final ListProperty<String> excludes;
    private final Property<Boolean> followLinks;
    private final Property<Boolean> useGradleConnector;
    private final DirectoryProperty outputDir;

    @Inject
    public GradleUsageExtension(ObjectFactory objects, ProjectLayout layout) {
        this.paths = objects.listProperty(String.class);
        this.excludes = objects.listProperty(String.class);
        this.followLinks = objects.property(Boolean.class).convention(false);
        this.useGradleConnector = objects.property(Boolean.class).convention(false);
        this.outputDir = objects.directoryProperty();
        this.outputDir.convention(layout.getBuildDirectory().dir("reports/usage"));
    }

    public List<String> getPaths() {
        return getPathsProperty().get();
    }

    public void path(String path) {
        paths.add(path);
    }

    ListProperty<String> getPathsProperty() {
        return paths;
    }

    public List<String> getExcludes() {
        return getExcludesProperty().get();
    }

    public void exclude(String exclude) {
        excludes.add(exclude);
    }

    ListProperty<String> getExcludesProperty() {
        return excludes;
    }

    public boolean getFollowLinks() {
        return getFollowLinksProperty().get();
    }

    public void setFollowLinks(boolean followLinks) {
        this.followLinks.set(followLinks);
    }

    Property<Boolean> getFollowLinksProperty() {
        return followLinks;
    }

    public boolean getUseGradleConnector() {
        return getUseGradleConnectorProperty().get();
    }

    public void setUseGradleConnector(boolean useGradleConnector) {
        this.useGradleConnector.set(useGradleConnector);
    }

    Property<Boolean> getUseGradleConnectorProperty() {
        return useGradleConnector;
    }

    public String getOutputDir() {
        return getOutputDirProperty().get().getAsFile().getAbsolutePath();
    }

    public void setOutputDir(String path) {
        outputDir.set(new File(path));
    }

    DirectoryProperty getOutputDirProperty() {
        return outputDir;
    }
}

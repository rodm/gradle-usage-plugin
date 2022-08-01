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

import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.tasks.TaskContainer;

public class GradleUsagePlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "usage";
    private static final String TASK_NAME = "usage";
    private static final String TASK_GROUP = "reports";
    private static final String TASK_DESCRIPTION = "Scans a directory and produces a report of all Gradle projects and the version used.";

    public void apply(Project project) {
        GradleUsageExtension extension = project.getExtensions().create(EXTENSION_NAME, GradleUsageExtension.class);

        TaskContainer tasks = project.getTasks();
        tasks.register(TASK_NAME, GradleUsageTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription(TASK_DESCRIPTION);
            task.getPaths().set(extension.getPathsProperty());
            task.getExcludes().set(extension.getExcludesProperty());
            task.getFollowLinks().set(extension.getFollowLinksProperty());
            task.getUseGradleConnector().set(extension.getUseGradleConnectorProperty());
            task.getOutputDirectory().set(extension.getOutputDirProperty());
        });
    }
}

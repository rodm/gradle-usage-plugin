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

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.nio.file.Files.exists;

public abstract class GradleUsageTask extends DefaultTask {

    static final String SETTINGS_GRADLE = "settings.gradle";
    static final String SETTINGS_GRADLE_KTS = "settings.gradle.kts";
    static final Path WRAPPER_PROPERTIES_FILE = Paths.get("gradle", "wrapper", "gradle-wrapper.properties");

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSourceDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Option(option = "dir", description = "The directory to scan for Gradle projects.")
    public void setDir(String path) {
        getSourceDirectory().set(getProject().file(path));
    }

    @TaskAction
    public void reportGradleUsage() {
        Path path = getSourceDirectory().getAsFile().get().toPath();
        List<GradleProject> projects = scanPath(path);
        List<String> output = produceReport(projects);
        storeReport(output, getOutputDirectory().file("usage.txt"));
    }

    private static boolean isGradleProject(Path path) {
        return exists(path.resolve(SETTINGS_GRADLE))
                || exists(path.resolve(SETTINGS_GRADLE_KTS))
                || exists(path.resolve(WRAPPER_PROPERTIES_FILE));
    }

    private static List<GradleProject> scanPath(Path path) {
        try {
            List<Path> gradleProjects = Files.walk(path)
                .filter(p -> p.toFile().isDirectory())
                .filter(GradleUsageTask::isGradleProject)
                .collect(Collectors.toList());

            List<GradleProject> projects = new ArrayList<>();
            for (Path gradleProject : gradleProjects) {
                String version = projectVersion(gradleProject);
                projects.add(new GradleProject(gradleProject, version));
            }
            return projects;
        }
        catch (IOException e) {
            throw new GradleException("Error scanning path", e);
        }
    }

    private static String projectVersion(Path path) {
        if (!exists(path.resolve(WRAPPER_PROPERTIES_FILE))) {
            return "UNKNOWN";
        }
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(path.toFile())
                .connect())
        {
            BuildEnvironment environment = connection.getModel(BuildEnvironment.class);
            return environment.getGradle().getGradleVersion();
        }
        catch (GradleConnectionException e) {
            return "UNKNOWN";
        }
    }

    private static List<String> produceReport(List<GradleProject> projects) {
        List<String> output = new ArrayList<>();
        produceProjectList(projects, output);
        produceSummary(projects, output);
        return output;
    }

    private static void produceProjectList(List<GradleProject> projects, List<String> output) {
        int width = maxVersionLength(projects);
        output.add(format("Found %d Gradle projects", projects.size()));
        projects.forEach(project -> output.add(format("  %" + width + "s  %s", project.version(), project.path())));
    }

    private static void produceSummary(List<GradleProject> projects, List<String> output) {
        Map<String, Long> summary = projects.stream()
                .map(GradleProject::version)
                .collect(Collectors.groupingBy(version -> version, Collectors.counting()));
        int width = maxVersionLength(projects);
        output.add("Summary");
        summary.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> output.add(format("  %" + width + "s used by %d projects", entry.getKey(), entry.getValue())));
    }

    private static int maxVersionLength(List<GradleProject> projects) {
        return projects.stream().mapToInt((project) -> project.version().length()).max().orElse(0);
    }

    private static void storeReport(List<String> projects, Provider<RegularFile> outputFile) {
        File output = outputFile.get().getAsFile();
        try {
            output.createNewFile();
            Files.write(output.toPath(), projects);
        }
        catch (IOException e) {
            throw new GradleException("Error creating report", e);
        }
    }
}

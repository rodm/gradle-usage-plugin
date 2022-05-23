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
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
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
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.rodm.gradle.usage.GradleProjectFinder.WRAPPER_PROPERTIES_FILE;
import static java.lang.String.format;
import static java.nio.file.Files.exists;

public abstract class GradleUsageTask extends DefaultTask {

    @Input
    public abstract ListProperty<String> getPaths();

    @Input
    public abstract ListProperty<String> getExcludes();

    @Input
    public abstract Property<Boolean> getFollowLinks();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Option(option = "dir", description = "A directory to scan for Gradle projects.")
    public void setDir(List<String> paths) {
        getPaths().addAll(paths);
    }

    @Option(option = "exclude-dir", description = "A directory to exclude from the scan for Gradle projects.")
    public void setExcludeDirs(List<String> paths) {
        getExcludes().addAll(paths);
    }

    @Option(option = "follow-links", description = "Configure the scanner to follow symbolic links.")
    public void setFollowLinksOption(boolean followLinks) {
        getFollowLinks().set(followLinks);
    }

    @TaskAction
    public void reportGradleUsage() {
        List<GradleProject> projects = scanPaths();
        List<String> output = produceReport(projects);
        storeReport(output, getOutputDirectory().file("usage.txt"));
    }

    private List<GradleProject> scanPaths() {
        try {
            Set<Path> paths = asPathSet(getPaths().get());
            Set<Path> excludes = asPathSet(getExcludes().get());
            validate(excludes);
            GradleProjectFinder finder = new GradleProjectFinder();
            List<Path> gradleProjects = finder.find(paths, excludes, getFollowLinks().get());

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

    private Set<Path> asPathSet(List<String> list) {
        return list.stream().map(Paths::get).collect(Collectors.toSet());
    }

    private void validate(Set<Path> paths) {
        paths.stream()
                .filter(Files::notExists)
                .forEach(path -> getLogger().warn("Invalid exclude path: {}", path));
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
            return "FAILED";
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

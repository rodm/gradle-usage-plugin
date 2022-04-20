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

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.Files.exists;
import static java.util.Collections.emptySet;

class GradleProjectFinder {

    static final String SETTINGS_GRADLE = "settings.gradle";
    static final String SETTINGS_GRADLE_KTS = "settings.gradle.kts";
    static final Path WRAPPER_PROPERTIES_FILE = Paths.get("gradle", "wrapper", "gradle-wrapper.properties");

    public List<Path> find(Path startPath, Set<Path> excludes, boolean followLinks) throws IOException {
        List<Path> paths = new ArrayList<>();
        Set<FileVisitOption> options = (followLinks) ? EnumSet.of(FOLLOW_LINKS) : emptySet();
        DirectoryVisitor visitor = new DirectoryVisitor(paths, normalizePaths(excludes));
        Files.walkFileTree(startPath, options, Integer.MAX_VALUE, visitor);
        return paths;
    }

    private Set<Path> normalizePaths(Set<Path> paths) {
        return paths.stream().map(Path::normalize).collect(Collectors.toSet());
    }

    static class DirectoryVisitor extends SimpleFileVisitor<Path> {

        private final List<Path> paths;
        private final Set<Path> excludes;

        public DirectoryVisitor(List<Path> paths, Set<Path> excludes) {
            this.paths = paths;
            this.excludes = excludes;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (excludes.contains(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            if (isGradleProject(dir)) {
                paths.add(dir);
            }
            return FileVisitResult.CONTINUE;
        }

        private static boolean isGradleProject(Path path) {
            return exists(path.resolve(SETTINGS_GRADLE))
                    || exists(path.resolve(SETTINGS_GRADLE_KTS))
                    || exists(path.resolve(WRAPPER_PROPERTIES_FILE));
        }
    }
}

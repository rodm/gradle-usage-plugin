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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.Files.exists;

class GradleProjectFinder {

    static final String SETTINGS_GRADLE = "settings.gradle";
    static final String SETTINGS_GRADLE_KTS = "settings.gradle.kts";
    static final Path WRAPPER_PROPERTIES_FILE = Paths.get("gradle", "wrapper", "gradle-wrapper.properties");

    public List<Path> find(Path startPath) throws IOException {
        return Files.walk(startPath)
                .filter(p -> p.toFile().isDirectory())
                .filter(GradleProjectFinder::isGradleProject)
                .collect(Collectors.toList());
    }

    private static boolean isGradleProject(Path path) {
        return exists(path.resolve(SETTINGS_GRADLE))
                || exists(path.resolve(SETTINGS_GRADLE_KTS))
                || exists(path.resolve(WRAPPER_PROPERTIES_FILE));
    }
}

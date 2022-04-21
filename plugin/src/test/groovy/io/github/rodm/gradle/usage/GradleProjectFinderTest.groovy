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
package io.github.rodm.gradle.usage

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path

import static io.github.rodm.gradle.usage.GradleProjectFinder.SETTINGS_GRADLE
import static io.github.rodm.gradle.usage.GradleProjectFinder.SETTINGS_GRADLE_KTS
import static io.github.rodm.gradle.usage.GradleProjectFinder.WRAPPER_PROPERTIES_FILE
import static java.nio.file.Files.createDirectories
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.not

class GradleProjectFinderTest {

    private Project project
    private GradleProjectFinder finder

    @TempDir
    private Path startPath

    private List<Path> find(Path startPath, boolean followLinks) {
        return find(startPath, [] as Set, followLinks)
    }

    private List<Path> find(Path startPath, Set<Path> excludes = [], boolean followLinks = false) {
        return find([startPath] as Set, excludes, followLinks)
    }

    private List<Path> find(Set<Path> paths, Set<Path> excludes = [], boolean followLinks = false) {
        return finder.find(paths, excludes, followLinks)
    }

    @BeforeEach
    void init() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply('io.github.rodm.gradle-usage')
        finder = new GradleProjectFinder()
    }

    @Test
    void 'returns zero projects'() {
        def projects = find(startPath)
        assertThat(projects, hasSize(0))
    }

    @Test
    void 'finds a project with a default settings file'() {
        File settingsFile = startPath.resolve(SETTINGS_GRADLE).toFile()
        settingsFile.createNewFile()

        def projects = find(startPath)
        assertThat(projects, hasSize(1))
    }

    @Test
    void 'finds a project with a Kotlin settings file'() {
        File settingsFile = startPath.resolve(SETTINGS_GRADLE_KTS).toFile()
        settingsFile.createNewFile()

        def projects = find(startPath)
        assertThat(projects, hasSize(1))
    }

    @Test
    void 'finds a project with a Wrapper properties file'() {
        File wrapperFile = startPath.resolve(WRAPPER_PROPERTIES_FILE).toFile()
        wrapperFile.parentFile.mkdirs()
        wrapperFile.createNewFile()

        def projects = find(startPath)
        assertThat(projects, hasSize(1))
    }

    @Test
    void 'finds multiple projects in a directory'() {
        Path projectsPath = startPath.resolve('projects')
        createGradleProject(projectsPath.resolve('project1'), SETTINGS_GRADLE)
        createGradleProject(projectsPath.resolve('project2'), SETTINGS_GRADLE_KTS)
        createGradleProject(projectsPath.resolve('project3'), WRAPPER_PROPERTIES_FILE)
        createGradleProject(projectsPath.resolve('project4'), 'dummy.txt')

        def projects = find(startPath)
        assertThat(projects, hasSize(3))
        assertThat(projects, hasItem(projectsPath.resolve('project1')))
        assertThat(projects, hasItem(projectsPath.resolve('project2')))
        assertThat(projects, hasItem(projectsPath.resolve('project3')))
        assertThat(projects, not(hasItem(projectsPath.resolve('project4'))))
    }

    @Test
    void 'finds multiple projects in multiple directories'() {
        Path path1 = startPath.resolve('dir1')
        createGradleProject(path1.resolve('project1'), SETTINGS_GRADLE)
        Path path2 = startPath.resolve('dir2')
        createGradleProject(path2.resolve('project2'), SETTINGS_GRADLE_KTS)

        def projects = find([path1, path2] as Set)
        assertThat(projects, hasSize(2))
        assertThat(projects, hasItem(path1.resolve('project1')))
        assertThat(projects, hasItem(path2.resolve('project2')))
    }

    @Test
    void 'finds multiple projects in a directory but excludes projects from excluded directory'() {
        Path projectsPath = startPath.resolve('projects')
        createGradleProject(projectsPath.resolve('project1'), SETTINGS_GRADLE)
        createGradleProject(projectsPath.resolve('project2'), SETTINGS_GRADLE_KTS)
        createGradleProject(projectsPath.resolve('folder/project3'), SETTINGS_GRADLE)
        createGradleProject(projectsPath.resolve('folder/project4'), SETTINGS_GRADLE)

        def excludes = [projectsPath.resolve('folder')] as Set
        def projects = find(startPath, excludes)

        assertThat(projects, hasSize(2))
        assertThat(projects, hasItem(projectsPath.resolve('project1')))
        assertThat(projects, hasItem(projectsPath.resolve('project2')))
        assertThat(projects, not(hasItem(projectsPath.resolve('folder/project3'))))
        assertThat(projects, not(hasItem(projectsPath.resolve('folder/project4'))))
    }

    @Test
    void 'finds project in a directory but excludes project from a normalized exclude directory'() {
        Path projectsPath = startPath.resolve('projects')
        createGradleProject(projectsPath.resolve('project1'), SETTINGS_GRADLE)
        createGradleProject(projectsPath.resolve('folder/project2'), SETTINGS_GRADLE)

        def excludes = [projectsPath.resolve('../projects/folder')] as Set
        def projects = find(startPath, excludes)

        assertThat(projects, hasSize(1))
        assertThat(projects, hasItem(projectsPath.resolve('project1')))
    }

    @Test
    void 'finds multiple projects in a directory and follows links'() {
        Path projectsPath = startPath.resolve('projects')
        createGradleProject(projectsPath.resolve('project1'), SETTINGS_GRADLE)
        createGradleProject(projectsPath.resolve('project2'), SETTINGS_GRADLE)
        Path project3 = startPath.resolve('folder/project3')
        createGradleProject(project3, SETTINGS_GRADLE)
        Files.createSymbolicLink(projectsPath.resolve('project3-link'), project3)

        def projects = find(startPath.resolve('projects'), true)

        assertThat(projects, hasItem(projectsPath.resolve('project1')))
        assertThat(projects, hasItem(projectsPath.resolve('project2')))
        assertThat(projects, hasItem(projectsPath.resolve('project3-link')))
    }

    @Test
    void 'finds multiple projects in a directory and does not follows links by default'() {
        Path projectsPath = startPath.resolve('projects')
        createGradleProject(projectsPath.resolve('project1'), SETTINGS_GRADLE)
        createGradleProject(projectsPath.resolve('project2'), SETTINGS_GRADLE)
        Path project3 = startPath.resolve('folder/project3')
        createGradleProject(project3, SETTINGS_GRADLE)
        Files.createSymbolicLink(projectsPath.resolve('project3-link'), project3)

        def projects = find(startPath.resolve('projects'), false)

        assertThat(projects, hasItem(projectsPath.resolve('project1')))
        assertThat(projects, hasItem(projectsPath.resolve('project2')))
        assertThat(projects, not(hasItem(projectsPath.resolve('project3-link'))))
    }

    static void createGradleProject(Path projectDir, String file) {
        createGradleProject(projectDir, projectDir.resolve(file))
    }

    static void createGradleProject(Path projectDir, Path file) {
        createDirectories(projectDir)
        createDirectories(projectDir.resolve(file).parent)
        projectDir.resolve(file) << """"""
    }
}

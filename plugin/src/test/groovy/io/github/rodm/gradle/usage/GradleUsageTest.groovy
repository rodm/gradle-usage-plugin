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

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.instanceOf
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue

class GradleUsageTest {

    private Project project

    @BeforeEach
    void init() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply('io.github.rodm.gradle-usage')
    }

    @Test
    void 'applying plugin registers a usage task'() {
        assertThat(project.tasks.findByName('usage'), notNullValue())
    }

    @Test
    void 'usage task has no default search paths'() {
        GradleUsageTask task = project.tasks.findByName('usage') as GradleUsageTask
        assertThat(task.paths.get(), is(empty()))
    }

    @Test
    void 'usage task has no default exclude paths'() {
        GradleUsageTask task = project.tasks.findByName('usage') as GradleUsageTask
        assertThat(task.excludes.get(), is(empty()))
    }

    @Test
    void 'usage task default is to not follow links'() {
        GradleUsageTask task = project.tasks.findByName('usage') as GradleUsageTask
        assertThat(task.followLinks.get(), is(false))
    }

    @Test
    void 'usage task default is to not use the Gradle wrapper version'() {
        GradleUsageTask task = project.tasks.findByName('usage') as GradleUsageTask
        assertThat(task.useWrapperVersion.get(), is(false))
    }

    @Test
    void 'usage task has a default output directory for its report'() {
        GradleUsageTask task = project.tasks.findByName('usage') as GradleUsageTask
        assertThat(task.outputDirectory.get().asFile.toString(), endsWith('build/reports/usage'))
    }

    @Test
    void 'applying plugin creates usage extension'() {
        def extension = project.extensions.getByName('usage')
        assertThat(extension, is(notNullValue()))
        assertThat(extension, instanceOf(GradleUsageExtension))
    }

    @Test
    void 'configures usage task with path'() {
        project.usage {
            path 'path1'
        }

        def task = project.tasks.getByName('usage') as GradleUsageTask
        assertThat(task.paths.get(), hasItem('path1'))
    }

    @Test
    void 'configures usage task with multiple paths'() {
        project.usage {
            path 'path1'
            path 'path2'
        }

        def task = project.tasks.getByName('usage') as GradleUsageTask
        assertThat(task.paths.get(), containsInAnyOrder('path1', 'path2'))
    }

    @Test
    void 'configures usage task with excluded path'() {
        project.usage {
            exclude 'path1'
        }

        def task = project.tasks.getByName('usage') as GradleUsageTask
        assertThat(task.excludes.get(), hasItem('path1'))
    }

    @Test
    void 'configures usage task with multiple excluded paths'() {
        project.usage {
            exclude 'path1'
            exclude 'path2'
        }

        def task = project.tasks.getByName('usage') as GradleUsageTask
        assertThat(task.excludes.get(), containsInAnyOrder('path1', 'path2'))
    }

    @Test
    void 'configures usage task with follows symlinks option'() {
        project.usage {
            followLinks = true
        }

        def task = project.tasks.getByName('usage') as GradleUsageTask
        assertThat(task.followLinks.get(), is(true))
    }

    @Test
    void 'configures usage task with use wrapper version option'() {
        project.usage {
            useWrapperVersion = true
        }

        def task = project.tasks.getByName('usage') as GradleUsageTask
        assertThat(task.useWrapperVersion.get(), is(true))
    }
}

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
package com.github.rodm.gradle.usage

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue

class GradleUsageTest {

    private Project project

    @BeforeEach
    void init() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply('com.github.rodm.gradle-usage')
    }

    @Test
    void 'applying plugin registers a usage task'() {
        assertThat(project.tasks.findByName('usage'), notNullValue())
    }

    @Test
    void 'usage task has no default source directory'() {
        GradleUsageTask task = project.tasks.findByName('usage') as GradleUsageTask
        assertThat(task.sourceDirectory.present, is(false))
    }

    @Test
    void 'usage task has a default output directory for its report'() {
        GradleUsageTask task = this.project.tasks.findByName('usage') as GradleUsageTask
        assertThat(task.outputDirectory.get().asFile.toString(), endsWith('build/reports/usage'))
    }
}

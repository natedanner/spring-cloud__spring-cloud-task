/*
 * Copyright 2018-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.batch.configuration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.batch.handler.TaskJobLauncherApplicationRunner;
import org.springframework.cloud.task.batch.listener.TaskBatchExecutionListenerTests;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class TaskJobLauncherAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BatchAutoConfiguration.class, TaskJobLauncherAutoConfiguration.class))
		.withUserConfiguration(TaskBatchExecutionListenerTests.JobConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, EmbeddedDataSourceConfiguration.class);

	@Test
	public void testAutoBuiltDataSourceWithTaskJobLauncherCLR() {
		this.contextRunner.withPropertyValues("spring.cloud.task.batch.fail-on-job-failure=true").run(context -> {
			assertThat(context).hasSingleBean(TaskJobLauncherApplicationRunner.class);
			assertThat(context.getBean(TaskJobLauncherApplicationRunner.class).getOrder()).isEqualTo(0);
		});
	}

	@Test
	public void testAutoBuiltDataSourceWithTaskJobLauncherCLROrder() {
		this.contextRunner
			.withPropertyValues("spring.cloud.task.batch.fail-on-job-failure=true",
					"spring.cloud.task.batch.applicationRunnerOrder=100")
			.run(context ->
				assertThat(context.getBean(TaskJobLauncherApplicationRunner.class).getOrder()).isEqualTo(100));
	}

	@Test
	public void testAutoBuiltDataSourceWithBatchJobNames() {
		this.contextRunner
			.withPropertyValues("spring.cloud.task.batch.fail-on-job-failure=true", "spring.batch.job.name=job1",
					"spring.cloud.task.batch.jobName=foobar")
			.run(context ->
				validateJobNames(context, "job1"));
	}

	@Test
	public void testAutoBuiltDataSourceWithTaskBatchJobNames() {
		this.contextRunner
			.withPropertyValues("spring.cloud.task.batch.fail-on-job-failure=true",
					"spring.cloud.task.batch.jobNames=job1,job2")
			.run(context ->
				validateJobNames(context, "job1,job2"));
	}

	private void validateJobNames(AssertableApplicationContext context, String jobNames) throws Exception {
		JobLauncherApplicationRunner jobLauncherApplicationRunner = context
			.getBean(TaskJobLauncherApplicationRunner.class);

		Object names = ReflectionTestUtils.getField(jobLauncherApplicationRunner, "jobName");
		assertThat(names).isEqualTo(jobNames);
	}

	@Test
	public void testAutoBuiltDataSourceWithTaskJobLauncherCLRDisabled() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(JobLauncherApplicationRunner.class);
			assertThat(context).doesNotHaveBean(TaskJobLauncherApplicationRunner.class);
		});
	}

}

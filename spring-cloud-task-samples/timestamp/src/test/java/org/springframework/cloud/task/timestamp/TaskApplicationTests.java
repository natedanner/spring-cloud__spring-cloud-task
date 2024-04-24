/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.task.timestamp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Task Application outputs the correct task log entries.
 *
 * @author Glenn Renfro
 */
@ExtendWith(OutputCaptureExtension.class)
public class TaskApplicationTests {

	@Test
	public void testTimeStampApp(CapturedOutput capturedOutput) throws Exception {
		final String testDateDots = ".......";
		final String createTaskMessage = "Creating: TaskExecution{executionId=";
		final String updateTaskMessage = "Updating: TaskExecution with executionId=";
		final String exitCodeMessage = "with the following {exitCode=0";
		String[] args = { "--format=yyyy" + testDateDots };

		SpringApplication.run(TaskApplication.class, args);

		String output = capturedOutput.toString();
		assertThat(output.contains(testDateDots)).as("Unable to find the timestamp: " + output).isTrue();
		assertThat(output.contains(createTaskMessage)).as("Test results do not show create task message: " + output)
			.isTrue();
		assertThat(output.contains(updateTaskMessage)).as("Test results do not show success message: " + output)
			.isTrue();
		assertThat(output.contains(exitCodeMessage)).as("Test results have incorrect exit code: " + output).isTrue();

		String taskTitle = " taskName='Demo Timestamp Task'";
		Pattern pattern = Pattern.compile(taskTitle);
		Matcher matcher = pattern.matcher(output);
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		assertThat(count).as("The number of task titles did not match expected: ").isEqualTo(1);
	}

}

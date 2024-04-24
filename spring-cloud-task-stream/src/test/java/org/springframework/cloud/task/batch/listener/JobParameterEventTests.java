/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.task.batch.listener;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobParameter;
import org.springframework.cloud.task.batch.listener.support.JobParameterEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class JobParameterEventTests {

	@Test
	public void testDefaultConstructor() {
		JobParameterEvent jobParameterEvent = new JobParameterEvent();
		assertThat(jobParameterEvent.getValue()).isNull();
		assertThat(jobParameterEvent.isIdentifying()).isFalse();
		assertThat(jobParameterEvent).isEqualTo(new JobParameterEvent());
	}

	@Test
	public void testConstructor() {
		final String expectedValue = "FOO";
		final LocalDateTime expectedDateValue = LocalDateTime.now();
		JobParameter jobParameter = new JobParameter(expectedValue, String.class);
		JobParameterEvent jobParameterEvent = new JobParameterEvent(jobParameter);
		assertThat(jobParameterEvent.getValue()).isEqualTo(expectedValue);
		assertThat(jobParameterEvent.isIdentifying()).isTrue();

		jobParameter = new JobParameter(expectedDateValue, LocalDateTime.class);
		jobParameterEvent = new JobParameterEvent(jobParameter);
		assertThat(jobParameterEvent.getValue()).isEqualTo(expectedDateValue);
		assertThat(jobParameterEvent.isIdentifying()).isTrue();
		assertThat(new JobParameterEvent(jobParameter).equals(jobParameterEvent)).isTrue();
	}

	@Test
	public void testEquals() {
		final String expectedValue = "FOO";
		JobParameter jobParameter = new JobParameter(expectedValue, String.class);
		JobParameterEvent jobParameterEvent = new JobParameterEvent(jobParameter);
		JobParameterEvent anotherJobParameterEvent = new JobParameterEvent(jobParameter);

		assertThat(jobParameterEvent.equals(jobParameterEvent)).isTrue();
		assertThat(jobParameterEvent.equals("nope")).isFalse();
		assertThat(jobParameterEvent.equals(anotherJobParameterEvent)).isTrue();
	}

	@Test
	public void testValidHashCode() {
		final String expectedValue = "FOO";
		JobParameter jobParameter = new JobParameter(expectedValue, String.class);
		JobParameterEvent jobParameterEvent = new JobParameterEvent(jobParameter);
		assertThat(jobParameterEvent.hashCode()).isNotNull();
	}

}

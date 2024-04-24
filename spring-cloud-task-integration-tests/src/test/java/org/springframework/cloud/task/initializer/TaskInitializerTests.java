/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.cloud.task.initializer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.task.executionid.TaskStartApplication;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Testcontainers
public class TaskInitializerTests {

	private static final int WAIT_INTERVAL = 500;

	private static final int MAX_WAIT_TIME = 5000;

	private DataSource dataSource;

	private TaskExplorer taskExplorer;

	private ConfigurableApplicationContext applicationContext;

	private static final DockerImageName MARIADB_IMAGE = DockerImageName.parse("mariadb:10.9.3");

	/**
	 * Provide mariadb test container for tests.
	 */
	@Container
	public static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_IMAGE);

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null && this.applicationContext.isActive()) {
			this.applicationContext.close();
		}
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(dataSource);
		this.taskExplorer = new SimpleTaskExplorer(factoryBean);
	}

	@BeforeEach
	public void setup() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(mariaDBContainer.getDriverClassName());
		dataSource.setUrl(mariaDBContainer.getJdbcUrl());
		dataSource.setUsername(mariaDBContainer.getUsername());
		dataSource.setPassword(mariaDBContainer.getPassword());
		this.dataSource = dataSource;
		TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(dataSource);
		this.taskExplorer = new SimpleTaskExplorer(factoryBean);

		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.execute("DROP TABLE IF EXISTS TASK_TASK_BATCH");
		template.execute("DROP TABLE IF EXISTS TASK_SEQ");
		template.execute("DROP TABLE IF EXISTS TASK_EXECUTION_PARAMS");
		template.execute("DROP TABLE IF EXISTS TASK_EXECUTION");
		template.execute("DROP TABLE IF EXISTS TASK_LOCK");
		template.execute("DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_SEQ");
		template.execute("DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_CONTEXT");
		template.execute("DROP TABLE IF EXISTS BATCH_STEP_EXECUTION");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_SEQ");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_SEQ");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_PARAMS");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_CONTEXT");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_EXECUTION");
		template.execute("DROP TABLE IF EXISTS BATCH_JOB_INSTANCE");
		template.execute("DROP SEQUENCE IF EXISTS TASK_SEQ");
	}

	@Test
	public void testNotInitialized() throws Exception {
		SpringApplication myapp = getTaskApplication();
		String[] properties = { "--spring.cloud.task.initialize-enabled=false" };
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> {
			this.applicationContext = myapp.run(properties);
		});
	}

	@Test
	public void testWithInitialized() throws Exception {
		SpringApplication myapp = getTaskApplication();
		String[] properties = { "--spring.cloud.task.initialize-enabled=true" };
		this.applicationContext = myapp.run(properties);
		assertThat(waitForDBToBePopulated()).isTrue();

		Page<TaskExecution> taskExecutions = this.taskExplorer.findAll(PageRequest.of(0, 10));
		TaskExecution te = taskExecutions.iterator().next();
		assertThat(taskExecutions.getTotalElements()).as("Only one row is expected").isEqualTo(1);
		assertThat(taskExecutions.iterator().next().getExitCode().intValue()).as("return code should be 0")
			.isEqualTo(0);
	}

	@Test
	public void testNotInitializedOriginalProperty() throws Exception {
		SpringApplication myapp = getTaskApplication();
		String[] properties = { "--spring.cloud.task.initialize.enable=false" };
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> {
			this.applicationContext = myapp.run(properties);
		});
	}

	@Test
	public void testWithInitializedOriginalProperty() throws Exception {
		SpringApplication myapp = getTaskApplication();
		String[] properties = { "--spring.cloud.task.initialize.enable=true" };
		this.applicationContext = myapp.run(properties);
		assertThat(waitForDBToBePopulated()).isTrue();

		Page<TaskExecution> taskExecutions = this.taskExplorer.findAll(PageRequest.of(0, 10));
		TaskExecution te = taskExecutions.iterator().next();
		assertThat(taskExecutions.getTotalElements()).as("Only one row is expected").isEqualTo(1);
		assertThat(taskExecutions.iterator().next().getExitCode().intValue()).as("return code should be 0")
			.isEqualTo(0);
	}

	private boolean tableExists() throws SQLException {
		boolean result;
		try (Connection conn = this.dataSource.getConnection();
				ResultSet res = conn.getMetaData().getTables(null, null, "TASK_EXECUTION", new String[] { "TABLE" })) {
			result = res.next();
		}
		return result;
	}

	private boolean waitForDBToBePopulated() throws Exception {
		boolean isDbPopulated = false;
		for (int waitTime = 0; waitTime <= MAX_WAIT_TIME; waitTime += WAIT_INTERVAL) {
			Thread.sleep(WAIT_INTERVAL);
			if (tableExists() && this.taskExplorer.getTaskExecutionCount() > 0) {
				isDbPopulated = true;
				break;
			}
		}
		return isDbPopulated;
	}

	private SpringApplication getTaskApplication() {
		SpringApplication myapp = new SpringApplication(TaskStartApplication.class);
		Map<String, Object> myMap = new HashMap<>();
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		myMap.put("spring.datasource.url", mariaDBContainer.getJdbcUrl());
		myMap.put("spring.datasource.username", mariaDBContainer.getUsername());
		myMap.put("spring.datasource.password", mariaDBContainer.getPassword());
		myMap.put("spring.datasource.driverClassName", mariaDBContainer.getDriverClassName());

		propertySources.addFirst(new MapPropertySource("EnvrionmentTestPropsource", myMap));
		myapp.setEnvironment(environment);
		return myapp;
	}

}

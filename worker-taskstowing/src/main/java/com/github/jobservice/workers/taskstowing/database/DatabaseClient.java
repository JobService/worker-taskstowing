/*
 * Copyright 2021 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jobservice.workers.taskstowing.database;

import com.github.jobservice.workers.taskstowing.factory.TaskStowingWorkerConfiguration;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public final class DatabaseClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseClient.class);
    private final Jdbi jdbi;
    private final String tableName;

    public DatabaseClient(final TaskStowingWorkerConfiguration configuration)
    {
        this.jdbi = Jdbi
            .create(HikariDataSourceFactory.createHikariDataSource(configuration))
            .installPlugin(new PostgresPlugin())
            .installPlugin(new SqlObjectPlugin());
        this.tableName = configuration.getDatabaseTableName();
    }

    public void checkHealth() throws Exception
    {
        jdbi.useHandle(handle -> {
            handle.execute("select version();");
        });
    }

    public void insertStowedTask(
        final String partitionId,
        final String jobId,
        final String taskClassifier,
        final int taskApiVersion,
        final byte[] taskData,
        final String taskStatus,
        final byte[] context,
        final String to,
        final byte[] trackingInfo,
        final byte[] sourceInfo,
        final String correlationId) throws Exception
    {
        jdbi.useHandle(handle -> {
            final StowedTaskDAO stowedTaskDAO = handle.attach(StowedTaskDAO.class);
            stowedTaskDAO.insertStowedTask(
                tableName,
                partitionId,
                jobId,
                taskClassifier,
                taskApiVersion,
                taskData,
                taskStatus,
                context,
                to,
                trackingInfo,
                sourceInfo,
                correlationId);
            LOGGER.info("Successfully stowed task for partition ID {} and job ID {}", partitionId, jobId);
        });
    }

    public void insertStowedTasks(
        final List<String> partitionIdList,
        final List<String> jobIdList,
        final List<String> taskClassifierList,
        final List<Integer> taskApiVersionList,
        final List<byte[]> taskDataList,
        final List<String> taskStatusList,
        final List<byte[]> contextList,
        final List<String> toList,
        final List<byte[]> trackingInfoList,
        final List<byte[]> sourceInfoList,
        final List<String> correlationIdList) throws Exception
    {
        jdbi.useHandle(handle -> {
            final StowedTaskDAO stowedTaskDAO = handle.attach(StowedTaskDAO.class);
            stowedTaskDAO.insertStowedTasks(
                tableName,
                partitionIdList,
                jobIdList,
                taskClassifierList,
                taskApiVersionList,
                taskDataList,
                taskStatusList,
                contextList,
                toList,
                trackingInfoList,
                sourceInfoList,
                correlationIdList);
            LOGGER.info("Successfully stowed {} task(s)", partitionIdList.size());
        });
    }
}

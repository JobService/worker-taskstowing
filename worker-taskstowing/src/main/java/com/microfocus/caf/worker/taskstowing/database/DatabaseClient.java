/*
 * Copyright 2021 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its
 * affiliates and licensors ("Micro Focus") are set forth in the express
 * warranty statements accompanying such products and services. Nothing
 * herein should be construed as constituting an additional warranty.
 * Micro Focus shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated
 * otherwise, a valid license is required for possession, use or copying.
 * Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial
 * Items are licensed to the U.S. Government under vendor's standard
 * commercial license.
 */
package com.microfocus.caf.worker.taskstowing.database;

import com.microfocus.caf.worker.taskstowing.factory.TaskStowingWorkerConfiguration;
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

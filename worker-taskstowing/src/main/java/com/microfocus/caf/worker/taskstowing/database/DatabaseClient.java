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

import com.microfocus.caf.worker.taskstowing.exceptions.UnexpectedNumberOfRowsInsertedException;
import com.microfocus.caf.worker.taskstowing.factory.TaskStowingWorkerConfiguration;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.microfocus.caf.worker.taskstowing.database.StowedTaskColumnName.*;
import org.jdbi.v3.postgres.PostgresPlugin;

public final class DatabaseClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseClient.class);
    private final Jdbi jdbi;
    private final String insertStowedTaskSql;

    public DatabaseClient(final TaskStowingWorkerConfiguration configuration)
    {
        this.jdbi = Jdbi.create(HikariDataSourceFactory.createHikariDataSource(configuration)).installPlugin(new PostgresPlugin());

        this.insertStowedTaskSql = String.format(
            "INSERT INTO %s (%s,%s,%s,%s,%s,%s,%s,\"%s\",%s,%s,%s) VALUES (:%s,:%s,:%s,:%s,:%s,:%s,:%s,:%s,:%s,:%s,:%s)",
            configuration.getDatabaseTableName(),
            PARTITION_ID,
            JOB_ID,
            TASK_CLASSIFIER,
            TASK_API_VERSION,
            TASK_DATA,
            TASK_STATUS,
            CONTEXT,
            TO,
            TRACKING_INFO,
            SOURCE_INFO,
            CORRELATION_ID,
            PARTITION_ID,
            JOB_ID,
            TASK_CLASSIFIER,
            TASK_API_VERSION,
            TASK_DATA,
            TASK_STATUS,
            CONTEXT,
            TO,
            TRACKING_INFO,
            SOURCE_INFO,
            CORRELATION_ID);
    }

    public void checkHealth() throws Exception
    {
        jdbi.useHandle(handle -> {
            handle.execute("select version();");
        });
    }

    public void stowTask(
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
            final Update update = handle.createUpdate(insertStowedTaskSql)
                .bind(PARTITION_ID, partitionId)
                .bind(JOB_ID, jobId)
                .bind(TASK_CLASSIFIER, taskClassifier)
                .bind(TASK_API_VERSION, taskApiVersion)
                .bind(TASK_DATA, taskData)
                .bind(TASK_STATUS, taskStatus)
                .bind(CONTEXT, context)
                .bind(TO, to)
                .bind(TRACKING_INFO, trackingInfo)
                .bind(SOURCE_INFO, sourceInfo)
                .bind(CORRELATION_ID, correlationId);

            final int numOfRowsInserted = update.execute();
            if (numOfRowsInserted == 1) {
                LOGGER.info("Successfully stowed task for partition ID {} and job ID {}", partitionId, jobId);
            } else {
                throw new UnexpectedNumberOfRowsInsertedException(
                    "Expected 1 row to be inserted into database, but was " + numOfRowsInserted);
            }
        });
    }
}

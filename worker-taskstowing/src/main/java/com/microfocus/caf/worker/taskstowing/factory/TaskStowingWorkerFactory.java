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
package com.microfocus.caf.worker.taskstowing.factory;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.worker.BulkWorker;
import com.hpe.caf.api.worker.BulkWorkerRuntime;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.JobStatus;
import com.hpe.caf.api.worker.NotIndendedTaskMessageForwardingEvaluator;
import com.hpe.caf.api.worker.TaskForwardingAction;
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerCallback;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerTask;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.microfocus.caf.worker.taskstowing.TaskStowingBulkWorker;
import com.microfocus.caf.worker.taskstowing.TaskStowingWorker;
import com.microfocus.caf.worker.taskstowing.database.DatabaseClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskStowingWorkerFactory implements WorkerFactory, NotIndendedTaskMessageForwardingEvaluator, BulkWorker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStowingWorkerFactory.class);

    private final TaskStowingWorkerConfiguration configuration;
    private final Codec codec;
    private final DatabaseClient databaseClient;

    public TaskStowingWorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec)
        throws WorkerException
    {
        try {
            this.configuration = configSource.getConfiguration(TaskStowingWorkerConfiguration.class);
        } catch (ConfigurationException e) {
            throw new WorkerException("Failed to load Task Stowing Worker configuration", e);
        }
        this.codec = Objects.requireNonNull(codec);
        this.databaseClient = new DatabaseClient(configuration);
    }

    @Override
    public Worker getWorker(WorkerTaskData workerTask) throws TaskRejectedException, InvalidTaskException
    {
        return new TaskStowingWorker(workerTask, configuration.getOutputQueue(), codec, databaseClient);
    }

    @Override
    public String getInvalidTaskQueue()
    {
        return configuration.getFailureQueue();
    }

    @Override
    public int getWorkerThreads()
    {
        return configuration.getThreads();
    }

    @Override
    public HealthResult healthCheck()
    {
        try {
            databaseClient.checkHealth();
            return HealthResult.RESULT_HEALTHY;
        } catch (final Exception exception) {
            final String message = "Database is unhealthy.";
            LOGGER.error(message, exception);
            return new HealthResult(HealthStatus.UNHEALTHY, message);
        }
    }

    @Override
    public TaskForwardingAction determineForwardingAction(
        final TaskMessage tm,
        final TaskInformation taskInformation,
        final boolean poison,
        final Map<String, Object> headers,
        final Codec codec,
        final JobStatus jobStatus,
        final WorkerCallback callback)
    {
        return jobStatus == JobStatus.Paused
            ? TaskForwardingAction.Execute
            : TaskForwardingAction.Forward;
    }

    @Override
    public void processTasks(final BulkWorkerRuntime bulkWorkerRuntime) throws InterruptedException
    {
        final long maxBatchTime = configuration.getMaxBatchTime();
        final int maxBatchSize = configuration.getMaxBatchSize();
        final long cutoffTime = System.currentTimeMillis() + maxBatchTime;
        final List<WorkerTask> workerTasks = new ArrayList<>();

        LOGGER.info("Starting to wait for tasks for bulk processing. Max batch size: {}. Max batch time: {}. Cut-off time: {}",
                    maxBatchSize, maxBatchTime, cutoffTime);
        for (;;) {
            final long maxWaitTime = cutoffTime - System.currentTimeMillis();
            LOGGER.info("Waiting for next task for bulk processing. Max wait time: {}", maxWaitTime);
            final WorkerTask workerTask = bulkWorkerRuntime.getNextWorkerTask(maxWaitTime);
            if (workerTask == null) {
                if (workerTasks.isEmpty()) {
                    LOGGER.info("No tasks received after max wait time: {}", maxWaitTime);
                } else {
                    LOGGER.info("No more tasks received after max wait time: {}. Will now begin to process: {} tasks.",
                                maxWaitTime, workerTasks.size());
                }
                break;
            }
            workerTasks.add(workerTask);
            LOGGER.info("Received task. Added task to bulk processing batch. Size of batch is now: {}. Max batch size: {}",
                        workerTasks.size(), maxBatchSize);
            if (workerTasks.size() >= maxBatchSize) {
                LOGGER.info("Current bulk processing batch size: {} is equal to or greater than the max batch size: {}. "
                    + "Will now begin to process tasks.", workerTasks.size(), maxBatchSize);
                break;
            }
        }

        if (workerTasks.size() > 0) {
            new TaskStowingBulkWorker(workerTasks, configuration.getOutputQueue(), codec, databaseClient).processTasks();
        }
    }
}

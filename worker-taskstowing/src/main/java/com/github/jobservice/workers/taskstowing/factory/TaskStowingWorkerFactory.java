/*
 * Copyright 2023 Open Text.
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
package com.github.jobservice.workers.taskstowing.factory;

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
import com.hpe.caf.api.worker.DivertedTaskHandler;
import com.hpe.caf.api.worker.DivertedTaskAction;
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerCallback;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerTask;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.github.jobservice.workers.taskstowing.TaskStowingBulkWorker;
import com.github.jobservice.workers.taskstowing.TaskStowingWorker;
import com.github.jobservice.workers.taskstowing.WorkerTaskHolder;
import com.github.jobservice.workers.taskstowing.database.DatabaseClient;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskStowingWorkerFactory implements WorkerFactory, DivertedTaskHandler, BulkWorker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStowingWorkerFactory.class);

    private final TaskStowingWorkerConfiguration configuration;
    private final String errorQueue;
    private final Codec codec;
    private final DatabaseClient databaseClient;

    public TaskStowingWorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec)
        throws WorkerException
    {
        try {
            this.configuration = configSource.getConfiguration(TaskStowingWorkerConfiguration.class);
        } catch (final ConfigurationException e) {
            throw new WorkerException("Failed to load Task Stowing Worker configuration", e);
        }
        this.errorQueue = configuration.getFailureQueue();
        this.codec = Objects.requireNonNull(codec);
        this.databaseClient = new DatabaseClient(configuration);
    }

    @Override
    public Worker getWorker(final WorkerTaskData workerTask) throws TaskRejectedException, InvalidTaskException
    {
        return new TaskStowingWorker(workerTask, errorQueue, codec, databaseClient);
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
    public DivertedTaskAction handleDivertedTask(
        final TaskMessage tm,
        final TaskInformation taskInformation,
        final boolean poison,
        final Map<String, Object> headers,
        final Codec codec,
        final JobStatus jobStatus,
        final WorkerCallback callback)
    {
        return jobStatus == JobStatus.Paused
            ? DivertedTaskAction.Execute
            : DivertedTaskAction.Forward;
    }

    @Override
    public void processTasks(final BulkWorkerRuntime bulkWorkerRuntime) throws InterruptedException
    {
        final long maxBatchTime = configuration.getMaxBatchTime();
        final int maxBatchSize = configuration.getMaxBatchSize();
        final long cutoffTime = System.currentTimeMillis() + maxBatchTime;
        final WorkerTaskHolder workerTaskHolder = new WorkerTaskHolder(errorQueue, codec);

        LOGGER.debug("Starting to collect tasks for bulk processing. Max batch size: {}. Max batch time: {}. Cut-off time: {}",
                     maxBatchSize, maxBatchTime, cutoffTime);
        for (;;) {
            final long maxWaitTime = cutoffTime - System.currentTimeMillis();
            LOGGER.debug("Waiting for next task for bulk processing. Max wait time: {}", maxWaitTime);
            final WorkerTask workerTask = bulkWorkerRuntime.getNextWorkerTask(maxWaitTime);
            if (workerTask == null) {
                if (workerTaskHolder.getWorkerTaskList().isEmpty()) {
                    LOGGER.debug("No valid tasks received after max wait time: {}", maxWaitTime);
                } else {
                    LOGGER.debug("No more valid tasks received after max wait time: {}. Will now begin to process: {} tasks.",
                                 maxWaitTime, workerTaskHolder.getWorkerTaskList().size());
                }
                break;
            }
            LOGGER.debug("Received task. Will now attempt to validate it before adding it to bulk processing batch.",
                         workerTaskHolder.getWorkerTaskList().size(), maxBatchSize);
            if (workerTaskHolder.addIfValid(workerTask)) {
                LOGGER.debug("Validated task. Added task to bulk processing batch. Size of batch is now: {}. Max batch size: {}",
                             workerTaskHolder.getWorkerTaskList().size(), maxBatchSize);
            }
            if (workerTaskHolder.getWorkerTaskList().size() >= maxBatchSize) {
                LOGGER.debug("Current bulk processing batch size: {} is equal to or greater than the max batch size: {}, "
                    + "so will now begin to process tasks.", workerTaskHolder.getWorkerTaskList().size(), maxBatchSize);
                break;
            }
        }

        if (workerTaskHolder.getWorkerTaskList().size() > 0) {
            new TaskStowingBulkWorker(workerTaskHolder, errorQueue, codec, databaseClient).processTasks();
        }
    }
}

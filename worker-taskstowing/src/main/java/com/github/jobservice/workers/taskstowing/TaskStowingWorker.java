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
package com.github.jobservice.workers.taskstowing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.hpe.caf.services.job.util.JobTaskId;
import com.github.jobservice.workers.taskstowing.database.DatabaseClient;
import com.github.jobservice.workers.taskstowing.database.DatabaseExceptionChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskStowingWorker implements Worker
{
    private static final String WORKER_IDENTIFIER = TaskStowingWorker.class.getSimpleName();
    private static final int WORKER_API_VERSION = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStowingWorker.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WorkerTaskData workerTaskData;
    private final String errorQueue;
    private final Codec codec;
    private final DatabaseClient databaseClient;

    public TaskStowingWorker(
        final WorkerTaskData workerTaskData,
        final String errorQueue,
        final Codec codec,
        final DatabaseClient databaseClient)
    {
        this.workerTaskData = workerTaskData;
        this.errorQueue = errorQueue;
        this.codec = codec;
        this.databaseClient = databaseClient;
    }

    @Override
    public WorkerResponse doWork() throws InterruptedException, TaskRejectedException, InvalidTaskException
    {
        LOGGER.info("Received request to stow task");

        final TrackingInfo trackingInfo = workerTaskData.getTrackingInfo();
        if (trackingInfo == null) {
            LOGGER.error(TaskStowingWorkerFailure.TRACKING_INFO_NOT_PRESENT);
            return createFailureResult(TaskStowingWorkerFailure.TRACKING_INFO_NOT_PRESENT);
        }
        final String jobTaskIdFromTrackingInfo = trackingInfo.getJobTaskId();
        if (Strings.isNullOrEmpty(jobTaskIdFromTrackingInfo)) {
            LOGGER.error(TaskStowingWorkerFailure.JOB_TASK_ID_NOT_PRESENT);
            return createFailureResult(TaskStowingWorkerFailure.JOB_TASK_ID_NOT_PRESENT);
        }
        final JobTaskId jobTaskId = JobTaskId.fromMessageId(jobTaskIdFromTrackingInfo);
        final String partitionId = jobTaskId.getPartitionId();
        if (Strings.isNullOrEmpty(partitionId)) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID);
            return createFailureResult(TaskStowingWorkerFailure.FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID);
        }
        final String jobId = jobTaskId.getJobId();
        if (Strings.isNullOrEmpty(jobId)) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID);
            return createFailureResult(TaskStowingWorkerFailure.FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID);
        }

        try {
            databaseClient.insertStowedTask(
                partitionId,
                jobId,
                workerTaskData.getClassifier(),
                workerTaskData.getVersion(),
                workerTaskData.getData(),
                workerTaskData.getStatus().name(),
                workerTaskData.getTo(),
                trackingInfo.getJobTaskId(),
                trackingInfo.getLastStatusCheckTime().getTime(),
                trackingInfo.getStatusCheckIntervalMillis(),
                trackingInfo.getStatusCheckUrl(),
                trackingInfo.getTrackingPipe(),
                trackingInfo.getTrackTo(),
                OBJECT_MAPPER.writeValueAsBytes(workerTaskData.getSourceInfo()),
                workerTaskData.getCorrelationId());
            return createSuccessResultNoOutputToQueue();
        } catch (final JsonProcessingException jsonProcessingException) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException);
            return createFailureResult(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException);
        } catch (final Exception exception) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception);
            if (DatabaseExceptionChecker.isTransientException(exception)) {
                throw new TaskRejectedException(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception);
            } else {
                return createFailureResult(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception);
            }
        }
    }

    @Override
    public String getWorkerIdentifier()
    {
        return WORKER_IDENTIFIER;
    }

    @Override
    public int getWorkerApiVersion()
    {
        return WORKER_API_VERSION;
    }

    @Override
    public WorkerResponse getGeneralFailureResult(final Throwable t)
    {
        return WorkerResponseFactory.createExceptionResult(errorQueue, WORKER_IDENTIFIER, WORKER_API_VERSION, codec, t);
    }

    private WorkerResponse createSuccessResultNoOutputToQueue()
    {
        return WorkerResponseFactory.createSuccessResultNoOutputToQueue(WORKER_IDENTIFIER, WORKER_API_VERSION);
    }

    private WorkerResponse createFailureResult(final String failure)
    {
        return WorkerResponseFactory.createFailureResult(errorQueue, WORKER_IDENTIFIER, WORKER_API_VERSION, codec, failure);
    }

    private WorkerResponse createFailureResult(final String failure, final Throwable cause)
    {
        return WorkerResponseFactory.createFailureResult(errorQueue, WORKER_IDENTIFIER, WORKER_API_VERSION, codec, failure, cause);
    }
}

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
package com.microfocus.caf.worker.taskstowing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.hpe.caf.services.job.util.JobTaskId;
import com.microfocus.caf.worker.taskstowing.database.DatabaseClient;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskStowingWorker implements Worker
{
    private static final String WORKER_IDENTIFIER = TaskStowingWorker.class.getSimpleName();
    private static final int WORKER_API_VERSION = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStowingWorker.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WorkerTaskData workerTaskData;
    private final String outputQueue;
    private final Codec codec;
    private final DatabaseClient databaseClient;

    public TaskStowingWorker(
        final WorkerTaskData workerTaskData,
        final String outputQueue,
        final Codec codec,
        final DatabaseClient databaseClient)
    {
        this.workerTaskData = workerTaskData;
        this.outputQueue = outputQueue;
        this.codec = codec;
        this.databaseClient = databaseClient;
    }

    @Override
    public WorkerResponse doWork() throws InterruptedException, TaskRejectedException, InvalidTaskException
    {
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
                workerTaskData.getContext() != null ? workerTaskData.getContext()
                : OBJECT_MAPPER.writeValueAsBytes(Collections.<String, byte[]>emptyMap()), // TODO check this: workerTaskData.getContext() always seems to be null, even when the task sent to the worker includes a non-null context?
                workerTaskData.getTo(),
                OBJECT_MAPPER.writeValueAsBytes(workerTaskData.getTrackingInfo()),
                OBJECT_MAPPER.writeValueAsBytes(workerTaskData.getSourceInfo()),
                workerTaskData.getCorrelationId());
            return createSuccessResultNoOutputToQueue();
        } catch (final JsonProcessingException jsonProcessingException) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException);
            return createFailureResult(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException);
        } catch (final Exception exception) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception);
            return createFailureResult(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception);
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
        return new WorkerResponse(
            outputQueue,
            TaskStatus.RESULT_EXCEPTION,
            WorkerResponseFactory.getExceptionData(t, codec),
            WORKER_IDENTIFIER,
            WORKER_API_VERSION,
            null);
    }

    private WorkerResponse createSuccessResultNoOutputToQueue()
    {
        return WorkerResponseFactory.createSuccessResultNoOutputToQueue(WORKER_IDENTIFIER, WORKER_API_VERSION);
    }

    private WorkerResponse createFailureResult(final String failure)
    {
        return WorkerResponseFactory.createFailureResult(outputQueue, WORKER_IDENTIFIER, WORKER_API_VERSION, codec, failure);
    }

    private WorkerResponse createFailureResult(final String failure, final Throwable cause)
    {
        return WorkerResponseFactory.createFailureResult(outputQueue, WORKER_IDENTIFIER, WORKER_API_VERSION, codec, failure, cause);
    }
}

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
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskFailedException;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.hpe.caf.services.job.util.JobTaskId;
import com.microfocus.caf.worker.taskstowing.database.DatabaseClient;
import java.util.Collections;
import java.util.Objects;
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
            return createFailureResult(TaskStowingWorkerFailure.TRACKING_INFO_NOT_PRESENT);
        }

        final String jobTaskIdFromTrackingInfo = trackingInfo.getJobTaskId();
        if (Strings.isNullOrEmpty(jobTaskIdFromTrackingInfo)) {
            return createFailureResult(TaskStowingWorkerFailure.JOB_TASK_ID_NOT_PRESENT);
        }

        final JobTaskId jobTaskId = JobTaskId.fromMessageId(jobTaskIdFromTrackingInfo);

        final String partitionId = jobTaskId.getPartitionId();
        if (Strings.isNullOrEmpty(partitionId)) {
            return createFailureResult(TaskStowingWorkerFailure.FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID);
        }

        final String jobId = jobTaskId.getJobId();
        if (Strings.isNullOrEmpty(jobId)) {
            return createFailureResult(TaskStowingWorkerFailure.FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID);
        }

        try {
            databaseClient.stowTask(
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
            return createFailureResult(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException);
        } catch (final Exception exception) {
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
            outputQueue, TaskStatus.RESULT_EXCEPTION, getExceptionData(t), getWorkerIdentifier(), getWorkerApiVersion(), null);
    }

    private WorkerResponse createSuccessResultNoOutputToQueue()
    {
        return new WorkerResponse(null, TaskStatus.RESULT_SUCCESS, new byte[]{}, getWorkerIdentifier(), getWorkerApiVersion(), null);
    }

    private WorkerResponse createFailureResult(final String failure)
    {
        LOGGER.error(failure);
        try {
            byte[] data = codec.serialise(failure);
            return new WorkerResponse(outputQueue, TaskStatus.RESULT_FAILURE, data, getWorkerIdentifier(), getWorkerApiVersion(), null);
        } catch (final CodecException e) {
            throw new TaskFailedException("Failed to serialise result", e);
        }
    }

    private WorkerResponse createFailureResult(final String failure, final Throwable cause)
    {
        LOGGER.error(failure, cause);
        final byte[] exceptionData = getExceptionData(new Exception(failure, cause));
        return new WorkerResponse(
            outputQueue, TaskStatus.RESULT_FAILURE, exceptionData, getWorkerIdentifier(), getWorkerApiVersion(), null);

    }

    private byte[] getExceptionData(final Throwable t)
    {
        try {
            String exceptionDetail = buildExceptionStackTrace(t);
            return codec.serialise(exceptionDetail);
        } catch (CodecException e) {
            LOGGER.warn("Failed to serialise exception, continuing", e);
            return new byte[]{};
        }
    }

    private static String buildExceptionStackTrace(final Throwable e)
    {
        // Build up exception detail from stack trace
        StringBuilder exceptionStackTrace = new StringBuilder(e.getClass() + " " + e.getMessage());
        // Check if there is a stack trace on the exception before building it up into a string
        if (Objects.nonNull(e.getStackTrace())) {
            exceptionStackTrace.append(stackTraceToString(e.getStackTrace()));
        }
        // If a cause exists add it to the exception detail
        if (Objects.nonNull(e.getCause())) {
            exceptionStackTrace.
                append(". Cause: ").append(e.getCause().getClass().toString()).append(" ").append(e.getCause().getMessage());
            // Check if the cause has a stack trace before building it up into a string
            if (Objects.nonNull(e.getCause().getStackTrace())) {
                exceptionStackTrace.append(stackTraceToString(e.getCause().getStackTrace()));
            }
        }
        return exceptionStackTrace.toString();
    }

    private static String stackTraceToString(final StackTraceElement[] stackTraceElements)
    {
        StringBuilder stackTraceStr = new StringBuilder();
        // From each stack trace element, build up the stack trace
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            stackTraceStr.append(" ").append(stackTraceElement.toString());
        }
        return stackTraceStr.toString();
    }
}

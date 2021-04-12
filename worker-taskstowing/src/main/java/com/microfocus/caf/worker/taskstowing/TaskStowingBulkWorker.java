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
import com.hpe.caf.api.worker.TaskFailedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTask;
import com.hpe.caf.services.job.util.JobTaskId;
import com.microfocus.caf.worker.taskstowing.database.DatabaseClient;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskStowingBulkWorker
{
    private static final String WORKER_IDENTIFIER = TaskStowingBulkWorker.class.getSimpleName();
    private static final int WORKER_API_VERSION = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStowingBulkWorker.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<WorkerTask> workerTasks;
    private final String outputQueue;
    private final Codec codec;
    private final DatabaseClient databaseClient;

    public TaskStowingBulkWorker(
        final List<WorkerTask> workerTasks,
        final String outputQueue,
        final Codec codec,
        final DatabaseClient databaseClient)
    {
        this.workerTasks = workerTasks;
        this.outputQueue = outputQueue;
        this.codec = codec;
        this.databaseClient = databaseClient;
    }

    public void processTasks()
    {
        // TODO batch these up into one SQL insert.
        for (final WorkerTask workerTask : workerTasks) {
            final TrackingInfo trackingInfo = workerTask.getTrackingInfo();
            if (trackingInfo == null) {
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.TRACKING_INFO_NOT_PRESENT));
                break;
            }

            final String jobTaskIdFromTrackingInfo = trackingInfo.getJobTaskId();
            if (Strings.isNullOrEmpty(jobTaskIdFromTrackingInfo)) {
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.JOB_TASK_ID_NOT_PRESENT));
                break;
            }

            final JobTaskId jobTaskId = JobTaskId.fromMessageId(jobTaskIdFromTrackingInfo);

            final String partitionId = jobTaskId.getPartitionId();
            if (Strings.isNullOrEmpty(partitionId)) {
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID));
                break;
            }

            final String jobId = jobTaskId.getJobId();
            if (Strings.isNullOrEmpty(jobId)) {
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID));
                break;
            }

            try {
                databaseClient.stowTask(
                    partitionId,
                    jobId,
                    workerTask.getClassifier(),
                    workerTask.getVersion(),
                    workerTask.getData(),
                    workerTask.getStatus().name(),
                    workerTask.getContext() != null ? workerTask.getContext()
                    : OBJECT_MAPPER.writeValueAsBytes(Collections.<String, byte[]>emptyMap()), // TODO check this: workerTaskData.getContext() always seems to be null, even when the task sent to the worker includes a non-null context?
                    workerTask.getTo(),
                    OBJECT_MAPPER.writeValueAsBytes(workerTask.getTrackingInfo()),
                    OBJECT_MAPPER.writeValueAsBytes(workerTask.getSourceInfo()),
                    workerTask.getCorrelationId());
                workerTask.setResponse(createSuccessResultNoOutputToQueue());
                break;
            } catch (final JsonProcessingException jsonProcessingException) {
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException));
                break;
            } catch (final Exception exception) {
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception));
                break;
            }
        }
    }

    public String getWorkerIdentifier()
    {
        return WORKER_IDENTIFIER;
    }

    public int getWorkerApiVersion()
    {
        return WORKER_API_VERSION;
    }

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

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
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.hpe.caf.services.job.util.JobTaskId;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.HealthMonitor;
import com.microfocus.caf.worker.taskstowing.database.DatabaseClient;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskStowingWorker implements DocumentWorker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStowingWorker.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DatabaseClient databaseClient;

    public TaskStowingWorker(final DatabaseClient databaseClient)
    {
        this.databaseClient = databaseClient;
    }

    @Override
    public void checkHealth(final HealthMonitor healthMonitor)
    {
        try {
            databaseClient.checkHealth();
        } catch (final Exception exception) {
            final String message = "Database is unhealthy";
            LOGGER.error(message, exception);
            healthMonitor.reportUnhealthy(message);
        }
    }

    @Override
    public void processDocument(final Document document) throws InterruptedException, DocumentWorkerTransientException
    {
        // TODO: Check forwarding behavior in WorkerCore, i.e. isTaskIndendedForThisWorker - this worker currently won't get this task.

        final WorkerTaskData workerTaskData = document.getTask().getService(WorkerTaskData.class);
        if (workerTaskData == null) {
            processFailure(TaskStowingWorkerFailure.WORKER_TASK_DATA_NOT_PRESENT, document);
            return;
        }

        final TrackingInfo trackingInfo = workerTaskData.getTrackingInfo();
        if (trackingInfo == null) {
            processFailure(TaskStowingWorkerFailure.TRACKING_INFO_NOT_PRESENT, document);
            return;
        }

        final String jobTaskIdFromTrackingInfo = trackingInfo.getJobTaskId();
        if (Strings.isNullOrEmpty(jobTaskIdFromTrackingInfo)) {
            processFailure(TaskStowingWorkerFailure.JOB_TASK_ID_NOT_PRESENT, document);
            return;
        }

        final JobTaskId jobTaskId = JobTaskId.fromMessageId(jobTaskIdFromTrackingInfo);

        final String partitionId = jobTaskId.getPartitionId();
        if (Strings.isNullOrEmpty(partitionId)) {
            processFailure(TaskStowingWorkerFailure.FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID, document);
            return;
        }

        final String jobId = jobTaskId.getJobId();
        if (Strings.isNullOrEmpty(jobId)) {
            processFailure(TaskStowingWorkerFailure.FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID, document);
            return;
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
        } catch (final JsonProcessingException jsonProcessingException) {
            processFailure(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException, document);
        } catch (final Exception exception) {
            processFailure(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception, document);
        }
    }

    private static void processFailure(
        final TaskStowingWorkerFailure taskStowingWorkerFailure,
        final Document document)
    {
        LOGGER.error(taskStowingWorkerFailure.getFailureMsg());
        taskStowingWorkerFailure.addToDoc(document);
    }

    private static void processFailure(
        final TaskStowingWorkerFailure taskStowingWorkerFailure,
        final Throwable cause,
        final Document document)
    {
        LOGGER.error(taskStowingWorkerFailure.getFailureMsg(), cause);
        taskStowingWorkerFailure.addToDoc(document, cause);
    }
}

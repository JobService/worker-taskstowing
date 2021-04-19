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
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTask;
import com.hpe.caf.services.job.util.JobTaskId;
import com.microfocus.caf.worker.taskstowing.database.DatabaseClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        LOGGER.info("Received request to stow {} task(s)", workerTasks.size());

        final List<WorkerTask> validatedWorkerTasks = new ArrayList<>();

        final List<String> partitionIdList = new ArrayList<>();
        final List<String> jobIdList = new ArrayList<>();
        final List<String> taskClassifierList = new ArrayList<>();
        final List<Integer> taskApiVersionList = new ArrayList<>();
        final List<byte[]> taskDataList = new ArrayList<>();
        final List<String> taskStatusList = new ArrayList<>();
        final List<byte[]> contextList = new ArrayList<>();
        final List<String> toList = new ArrayList<>();
        final List<byte[]> trackingInfoList = new ArrayList<>();
        final List<byte[]> sourceInfoList = new ArrayList<>();
        final List<String> correlationIdList = new ArrayList<>();

        for (final WorkerTask workerTask : workerTasks) {
            final TrackingInfo trackingInfo = workerTask.getTrackingInfo();
            if (trackingInfo == null) {
                LOGGER.error(TaskStowingWorkerFailure.TRACKING_INFO_NOT_PRESENT);
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.TRACKING_INFO_NOT_PRESENT));
                continue;
            }
            final String jobTaskIdFromTrackingInfo = trackingInfo.getJobTaskId();
            if (Strings.isNullOrEmpty(jobTaskIdFromTrackingInfo)) {
                LOGGER.error(TaskStowingWorkerFailure.JOB_TASK_ID_NOT_PRESENT);
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.JOB_TASK_ID_NOT_PRESENT));
                continue;
            }
            final JobTaskId jobTaskId = JobTaskId.fromMessageId(jobTaskIdFromTrackingInfo);
            final String partitionId = jobTaskId.getPartitionId();
            if (Strings.isNullOrEmpty(partitionId)) {
                LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID);
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID));
                continue;
            }
            final String jobId = jobTaskId.getJobId();
            if (Strings.isNullOrEmpty(jobId)) {
                LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID);
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID));
                continue;
            }

            try {
                // TODO check this: workerTaskData.getContext() always seems to be null, even when the task sent to the worker includes a non-null context?
                final byte[] contextBytes = workerTask.getContext() != null
                    ? workerTask.getContext()
                    : OBJECT_MAPPER.writeValueAsBytes(Collections.<String, byte[]>emptyMap());
                final byte[] trackingInfoBytes = OBJECT_MAPPER.writeValueAsBytes(workerTask.getTrackingInfo());
                final byte[] sourceInfoBytes = OBJECT_MAPPER.writeValueAsBytes(workerTask.getSourceInfo());

                partitionIdList.add(partitionId);
                jobIdList.add(jobId);
                taskClassifierList.add(workerTask.getClassifier());
                taskApiVersionList.add(workerTask.getVersion());
                taskDataList.add(workerTask.getData());
                taskStatusList.add(workerTask.getStatus().name());
                contextList.add(contextBytes);
                toList.add(workerTask.getTo());
                trackingInfoList.add(trackingInfoBytes);
                sourceInfoList.add(sourceInfoBytes);
                correlationIdList.add(workerTask.getCorrelationId());

                validatedWorkerTasks.add(workerTask);
            } catch (final JsonProcessingException jsonProcessingException) {
                LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException);
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException));
            }
        }

        if (validatedWorkerTasks.isEmpty()) {
            // An error will already have been logged and a response set on each task that failed validation, so just return.
            return;
        }

        if (anyListsEmptyOrNotSameSize(validatedWorkerTasks,
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
                                       correlationIdList)) {
            // Should not happen normally, but want to make sure we don't go ahead with the database insertion if it does.
            LOGGER.error(TaskStowingWorkerFailure.UNKNOWN_ERROR);
            validatedWorkerTasks.forEach(workerTask -> {
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.UNKNOWN_ERROR));
            });
            return;
        }

        LOGGER.info("{}/{} task(s) were validated without errors, will now attempt to stow {} task(s)",
                    validatedWorkerTasks.size(), workerTasks.size(), validatedWorkerTasks.size());
        try {
            databaseClient.insertStowedTasks(
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

            validatedWorkerTasks.forEach(workerTask -> {
                workerTask.setResponse(createSuccessResultNoOutputToQueue());
            });
        } catch (final Exception exception) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception);
            validatedWorkerTasks.forEach(workerTask -> {
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception));
            });
        }
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

    private static boolean anyListsEmptyOrNotSameSize(
        final List<WorkerTask> validatedWorkerTasks,
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
        final List<String> correlationIdList)
    {
        final List<List<? extends Object>> lists = new ArrayList<>();
        lists.add(validatedWorkerTasks);
        lists.add(partitionIdList);
        lists.add(jobIdList);
        lists.add(taskClassifierList);
        lists.add(taskApiVersionList);
        lists.add(taskDataList);
        lists.add(taskStatusList);
        lists.add(contextList);
        lists.add(toList);
        lists.add(trackingInfoList);
        lists.add(sourceInfoList);
        lists.add(correlationIdList);

        return !lists.stream().allMatch(list -> !list.isEmpty() && (list.size() == lists.get(0).size()));
    }
}

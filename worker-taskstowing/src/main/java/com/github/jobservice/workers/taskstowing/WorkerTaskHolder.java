/*
 * Copyright 2021-2024 Open Text.
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
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTask;
import com.hpe.caf.services.job.util.JobTaskId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkerTaskHolder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerTaskHolder.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String errorQueue;
    private final Codec codec;

    private final List<WorkerTask> workerTaskList;
    private final List<String> partitionIdList;
    private final List<String> jobIdList;
    private final List<String> taskClassifierList;
    private final List<Integer> taskApiVersionList;
    private final List<byte[]> taskDataList;
    private final List<String> taskStatusList;
    private final List<String> toList;
    private final List<String> trackingInfoJobTaskIdList;
    private final List<Long> trackingInfoLastStatusCheckTimeList;
    private final List<Long> trackingInfoStatusCheckIntervalMillisList;
    private final List<String> trackingInfoStatusCheckUrlList;
    private final List<String> trackingInfoTrackingPipeList;
    private final List<String> trackingInfoTrackToList;
    private final List<byte[]> sourceInfoList;
    private final List<String> correlationIdList;

    public WorkerTaskHolder(final String errorQueue, final Codec codec)
    {
        this.errorQueue = errorQueue;
        this.codec = codec;

        this.workerTaskList = new ArrayList<>();
        this.partitionIdList = new ArrayList<>();
        this.jobIdList = new ArrayList<>();
        this.taskClassifierList = new ArrayList<>();
        this.taskApiVersionList = new ArrayList<>();
        this.taskDataList = new ArrayList<>();
        this.taskStatusList = new ArrayList<>();
        this.toList = new ArrayList<>();
        this.trackingInfoJobTaskIdList = new ArrayList<>();
        this.trackingInfoLastStatusCheckTimeList = new ArrayList<>();
        this.trackingInfoStatusCheckIntervalMillisList = new ArrayList<>();
        this.trackingInfoStatusCheckUrlList = new ArrayList<>();
        this.trackingInfoTrackingPipeList = new ArrayList<>();
        this.trackingInfoTrackToList = new ArrayList<>();
        this.sourceInfoList = new ArrayList<>();
        this.correlationIdList = new ArrayList<>();
    }

    public boolean addIfValid(final WorkerTask workerTask)
    {
        final TrackingInfo trackingInfo = workerTask.getTrackingInfo();
        if (trackingInfo == null) {
            LOGGER.error(TaskStowingWorkerFailure.TRACKING_INFO_NOT_PRESENT);
            workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.TRACKING_INFO_NOT_PRESENT));
            return false;
        }

        final String jobTaskIdFromTrackingInfo = trackingInfo.getJobTaskId();
        if (Strings.isNullOrEmpty(jobTaskIdFromTrackingInfo)) {
            LOGGER.error(TaskStowingWorkerFailure.JOB_TASK_ID_NOT_PRESENT);
            workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.JOB_TASK_ID_NOT_PRESENT));
            return false;
        }

        final JobTaskId jobTaskId = JobTaskId.fromMessageId(jobTaskIdFromTrackingInfo);
        final String partitionId = jobTaskId.getPartitionId();
        if (Strings.isNullOrEmpty(partitionId)) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID);
            workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID));
            return false;
        }
        final String jobId = jobTaskId.getJobId();
        if (Strings.isNullOrEmpty(jobId)) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID);
            workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID));
            return false;
        }

        try {
            workerTaskList.add(workerTask);
            partitionIdList.add(partitionId);
            jobIdList.add(jobId);
            taskClassifierList.add(workerTask.getClassifier());
            taskApiVersionList.add(workerTask.getVersion());
            taskDataList.add(workerTask.getData());
            taskStatusList.add(workerTask.getStatus().name());
            toList.add(workerTask.getTo());
            trackingInfoJobTaskIdList.add(workerTask.getTrackingInfo().getJobTaskId());
            trackingInfoLastStatusCheckTimeList.add(workerTask.getTrackingInfo().getLastStatusCheckTime().getTime());
            trackingInfoStatusCheckIntervalMillisList.add(workerTask.getTrackingInfo().getStatusCheckIntervalMillis());
            trackingInfoStatusCheckUrlList.add(workerTask.getTrackingInfo().getStatusCheckUrl());
            trackingInfoTrackingPipeList.add(workerTask.getTrackingInfo().getTrackingPipe());
            trackingInfoTrackToList.add(workerTask.getTrackingInfo().getTrackTo());
            sourceInfoList.add(OBJECT_MAPPER.writeValueAsBytes(workerTask.getSourceInfo()));
            correlationIdList.add(workerTask.getCorrelationId());
            return true;
        } catch (final JsonProcessingException jsonProcessingException) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException);
            workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_SERIALIZE_TASK, jsonProcessingException));
            return false;
        }
    }

    private WorkerResponse createFailureResult(final String failure)
    {
        return WorkerResponseFactory.createFailureResult(
            errorQueue, TaskStowingBulkWorker.WORKER_IDENTIFIER, TaskStowingBulkWorker.WORKER_API_VERSION, codec, failure);
    }

    private WorkerResponse createFailureResult(final String failure, final Throwable cause)
    {
        return WorkerResponseFactory.createFailureResult(
            errorQueue, TaskStowingBulkWorker.WORKER_IDENTIFIER, TaskStowingBulkWorker.WORKER_API_VERSION, codec, failure, cause);
    }

    public List<WorkerTask> getWorkerTaskList()
    {
        return workerTaskList;
    }

    public List<String> getPartitionIdList()
    {
        return partitionIdList;
    }

    public List<String> getJobIdList()
    {
        return jobIdList;
    }

    public List<String> getTaskClassifierList()
    {
        return taskClassifierList;
    }

    public List<Integer> getTaskApiVersionList()
    {
        return taskApiVersionList;
    }

    public List<byte[]> getTaskDataList()
    {
        return taskDataList;
    }

    public List<String> getTaskStatusList()
    {
        return taskStatusList;
    }

    public List<String> getToList()
    {
        return toList;
    }

    public List<String> getTrackingInfoJobTaskIdList()
    {
        return trackingInfoJobTaskIdList;
    }

    public List<Long> getTrackingInfoLastStatusCheckTimeList()
    {
        return trackingInfoLastStatusCheckTimeList;
    }

    public List<Long> getTrackingInfoStatusCheckIntervalMillisList()
    {
        return trackingInfoStatusCheckIntervalMillisList;
    }

    public List<String> getTrackingInfoStatusCheckUrlList()
    {
        return trackingInfoStatusCheckUrlList;
    }

    public List<String> getTrackingInfoTrackingPipeList()
    {
        return trackingInfoTrackingPipeList;
    }

    public List<String> getTrackingInfoTrackToList()
    {
        return trackingInfoTrackToList;
    }

    public List<byte[]> getSourceInfoList()
    {
        return sourceInfoList;
    }

    public List<String> getCorrelationIdList()
    {
        return correlationIdList;
    }
}

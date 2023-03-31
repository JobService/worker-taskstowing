/*
 * Copyright 2021-2023 Open Text.
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

final class StowedTaskRow
{
    private final String partitionId;
    private final String jobId;
    private final String taskClassifier;
    private final int taskApiVersion;
    private final byte[] taskData;
    private final String taskStatus;
    private final String to;
    private final String trackingInfoJobTaskId;
    private final Long trackingInfoLastStatusCheckTime;
    private final Long trackingInfoStatusCheckIntervalMillis;
    private final String trackingInfoStatusCheckUrl;
    private final String trackingInfoTrackingPipe;
    private final String trackingInfoTrackTo;
    private final byte[] sourceInfo;
    private final String correlationId;

    public StowedTaskRow(
        final String partitionId,
        final String jobId,
        final String taskClassifier,
        final int taskApiVersion,
        final byte[] taskData,
        final String taskStatus,
        final String to,
        final String trackingInfoJobTaskId,
        final Long trackingInfoLastStatusCheckTime,
        final Long trackingInfoStatusCheckIntervalMillis,
        final String trackingInfoStatusCheckUrl,
        final String trackingInfoTrackingPipe,
        final String trackingInfoTrackTo,
        final byte[] sourceInfo,
        final String correlationId)
    {
        this.partitionId = partitionId;
        this.jobId = jobId;
        this.taskClassifier = taskClassifier;
        this.taskApiVersion = taskApiVersion;
        this.taskData = taskData;
        this.taskStatus = taskStatus;
        this.to = to;
        this.trackingInfoLastStatusCheckTime = trackingInfoLastStatusCheckTime;
        this.trackingInfoJobTaskId = trackingInfoJobTaskId;
        this.trackingInfoStatusCheckIntervalMillis = trackingInfoStatusCheckIntervalMillis;
        this.trackingInfoStatusCheckUrl = trackingInfoStatusCheckUrl;
        this.trackingInfoTrackingPipe = trackingInfoTrackingPipe;
        this.trackingInfoTrackTo = trackingInfoTrackTo;
        this.sourceInfo = sourceInfo;
        this.correlationId = correlationId;
    }

    public String getPartitionId()
    {
        return partitionId;
    }

    public String getJobId()
    {
        return jobId;
    }

    public String getTaskClassifier()
    {
        return taskClassifier;
    }

    public int getTaskApiVersion()
    {
        return taskApiVersion;
    }

    public byte[] getTaskData()
    {
        return taskData;
    }

    public String getTaskStatus()
    {
        return taskStatus;
    }

    public String getTo()
    {
        return to;
    }

    public String getTrackingInfoJobTaskId()
    {
        return trackingInfoJobTaskId;
    }

    public Long getTrackingInfoLastStatusCheckTime()
    {
        return trackingInfoLastStatusCheckTime;
    }

    public Long getTrackingInfoStatusCheckIntervalMillis()
    {
        return trackingInfoStatusCheckIntervalMillis;
    }

    public String getTrackingInfoStatusCheckUrl()
    {
        return trackingInfoStatusCheckUrl;
    }

    public String getTrackingInfoTrackingPipe()
    {
        return trackingInfoTrackingPipe;
    }

    public String getTrackingInfoTrackTo()
    {
        return trackingInfoTrackTo;
    }

    public byte[] getSourceInfo()
    {
        return sourceInfo;
    }

    public String getCorrelationId()
    {
        return correlationId;
    }
}

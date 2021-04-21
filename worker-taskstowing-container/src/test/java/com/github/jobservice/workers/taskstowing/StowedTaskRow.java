/*
 * Copyright 2021 Micro Focus or one of its affiliates.
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
    private final Long id;
    private final String partitionId;
    private final String jobId;
    private final String taskClassifier;
    private final int taskApiVersion;
    private final byte[] taskData;
    private final String taskStatus;
    private final byte[] context;
    private final String to;
    private final byte[] trackingInfo;
    private final byte[] sourceInfo;
    private final String correlationId;

    public StowedTaskRow(
        final Long id,
        final String partitionId,
        final String jobId,
        final String taskClassifier,
        final int taskApiVersion,
        final byte[] taskData,
        final String taskStatus,
        final byte[] context,
        final String to,
        final byte[] trackingInfo,
        final byte[] sourceInfo,
        final String correlationId)
    {
        this.id = id;
        this.partitionId = partitionId;
        this.jobId = jobId;
        this.taskClassifier = taskClassifier;
        this.taskApiVersion = taskApiVersion;
        this.taskData = taskData;
        this.taskStatus = taskStatus;
        this.context = context;
        this.to = to;
        this.trackingInfo = trackingInfo;
        this.sourceInfo = sourceInfo;
        this.correlationId = correlationId;
    }

    public Long getId()
    {
        return id;
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

    public byte[] getContext()
    {
        return context;
    }

    public String getTo()
    {
        return to;
    }

    public byte[] getTrackingInfo()
    {
        return trackingInfo;
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

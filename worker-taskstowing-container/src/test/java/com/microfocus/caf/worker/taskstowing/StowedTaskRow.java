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

public final class StowedTaskRow
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

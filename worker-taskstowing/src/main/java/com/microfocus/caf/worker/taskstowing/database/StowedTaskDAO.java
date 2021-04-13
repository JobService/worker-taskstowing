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
package com.microfocus.caf.worker.taskstowing.database;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import static com.microfocus.caf.worker.taskstowing.database.StowedTaskColumnName.*;

interface StowedTaskDAO
{
    @SqlUpdate("insert into <table> "
        + "("
        + PARTITION_ID + ", "
        + JOB_ID + ", "
        + TASK_CLASSIFIER + ", "
        + TASK_API_VERSION + ", "
        + TASK_DATA + ", "
        + TASK_STATUS + ", "
        + CONTEXT + ", \""
        + TO + "\", "
        + TRACKING_INFO + ", "
        + SOURCE_INFO + ", "
        + CORRELATION_ID + ") values "
        + "(:partitionId, :jobId, :taskClassifier, :taskApiVersion, :taskData, :taskStatus, :context, :to, :trackingInfo, :sourceInfo, :correlationId)")
    void insertStowedTask(@Define("table") final String table,
                          @Bind("partitionId") final String partitionId,
                          @Bind("jobId") final String jobId,
                          @Bind("taskClassifier") final String taskClassifier,
                          @Bind("taskApiVersion") final Integer taskApiVersion,
                          @Bind("taskData") final byte[] taskData,
                          @Bind("taskStatus") final String taskStatus,
                          @Bind("context") final byte[] context,
                          @Bind("to") final String to,
                          @Bind("trackingInfo") final byte[] trackingInfo,
                          @Bind("sourceInfo") final byte[] sourceInfo,
                          @Bind("correlationId") final String correlationId);

    @SqlBatch("insert into <table> "
        + "("
        + PARTITION_ID + ", "
        + JOB_ID + ", "
        + TASK_CLASSIFIER + ", "
        + TASK_API_VERSION + ", "
        + TASK_DATA + ", "
        + TASK_STATUS + ", "
        + CONTEXT + ", \""
        + TO + "\", "
        + TRACKING_INFO + ", "
        + SOURCE_INFO + ", "
        + CORRELATION_ID + ") values "
        + "(:partitionId, :jobId, :taskClassifier, :taskApiVersion, :taskData, :taskStatus, :context, :to, :trackingInfo, :sourceInfo, :correlationId)")
    void insertStowedTasks(@Define("table") final String table,
                           @Bind("partitionId") final List<String> partitionIdList,
                           @Bind("jobId") final List<String> jobIdList,
                           @Bind("taskClassifier") final List<String> taskClassifierList,
                           @Bind("taskApiVersion") final List<Integer> taskApiVersionList,
                           @Bind("taskData") final List<byte[]> taskDataList,
                           @Bind("taskStatus") final List<String> taskStatusList,
                           @Bind("context") final List<byte[]> contextList,
                           @Bind("to") final List<String> toList,
                           @Bind("trackingInfo") final List<byte[]> trackingInfoList,
                           @Bind("sourceInfo") final List<byte[]> sourceInfoList,
                           @Bind("correlationId") final List<String> correlationIdList);

}

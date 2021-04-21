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
package com.github.jobservice.workers.taskstowing.database;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import static com.github.jobservice.workers.taskstowing.database.StowedTaskColumnName.*;

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
        + TRACKING_INFO_JOB_TASK_ID + ", "
        + TRACKING_INFO_LAST_STATUS_CHECK_TIME + ", "
        + TRACKING_INFO_STATUS_CHECK_INTERVAL_MILLIS + ", "
        + TRACKING_INFO_STATUS_CHECK_URL + ", "
        + TRACKING_INFO_TRACKING_PIPE + ", "
        + TRACKING_INFO_TRACK_TO + ", "
        + SOURCE_INFO + ", "
        + CORRELATION_ID + ") values "
        + "(:partitionId, :jobId, :taskClassifier, :taskApiVersion, :taskData, :taskStatus, :context, :to, :trackingInfoJobTaskId, "
        + ":trackingInfoLastStatusCheckTime, :trackingInfoStatusCheckIntervalMillis, :trackingInfoStatusCheckUrl, "
        + ":trackingInfoTrackingPipe, :trackingInfoTrackTo, :sourceInfo, :correlationId)")
    void insertStowedTask(@Define("table") final String table,
                          @Bind("partitionId") final String partitionId,
                          @Bind("jobId") final String jobId,
                          @Bind("taskClassifier") final String taskClassifier,
                          @Bind("taskApiVersion") final Integer taskApiVersion,
                          @Bind("taskData") final byte[] taskData,
                          @Bind("taskStatus") final String taskStatus,
                          @Bind("context") final byte[] context,
                          @Bind("to") final String to,
                          @Bind("trackingInfoJobTaskId") final String trackingInfoJobTaskId,
                          @Bind("trackingInfoLastStatusCheckTime") final Long trackingInfoLastStatusCheckTime,
                          @Bind("trackingInfoStatusCheckIntervalMillis") final Long trackingInfoStatusCheckIntervalMillis,
                          @Bind("trackingInfoStatusCheckUrl") final String trackingInfoStatusCheckUrl,
                          @Bind("trackingInfoTrackingPipe") final String trackingInfoTrackingPipe,
                          @Bind("trackingInfoTrackTo") final String trackingInfoTrackTo,
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
        + TRACKING_INFO_JOB_TASK_ID + ", "
        + TRACKING_INFO_LAST_STATUS_CHECK_TIME + ", "
        + TRACKING_INFO_STATUS_CHECK_INTERVAL_MILLIS + ", "
        + TRACKING_INFO_STATUS_CHECK_URL + ", "
        + TRACKING_INFO_TRACKING_PIPE + ", "
        + TRACKING_INFO_TRACK_TO + ", "
        + SOURCE_INFO + ", "
        + CORRELATION_ID + ") values "
        + "(:partitionId, :jobId, :taskClassifier, :taskApiVersion, :taskData, :taskStatus, :context, :to, :trackingInfoJobTaskId, "
        + ":trackingInfoLastStatusCheckTime, :trackingInfoStatusCheckIntervalMillis, :trackingInfoStatusCheckUrl, "
        + ":trackingInfoTrackingPipe, :trackingInfoTrackTo, :sourceInfo, :correlationId)")
    void insertStowedTasks(@Define("table") final String table,
                           @Bind("partitionId") final List<String> partitionIdList,
                           @Bind("jobId") final List<String> jobIdList,
                           @Bind("taskClassifier") final List<String> taskClassifierList,
                           @Bind("taskApiVersion") final List<Integer> taskApiVersionList,
                           @Bind("taskData") final List<byte[]> taskDataList,
                           @Bind("taskStatus") final List<String> taskStatusList,
                           @Bind("context") final List<byte[]> contextList,
                           @Bind("to") final List<String> toList,
                           @Bind("trackingInfoJobTaskId") final List<String> trackingInfoJobTaskId,
                           @Bind("trackingInfoLastStatusCheckTime") final List<Long> trackingInfoLastStatusCheckTime,
                           @Bind("trackingInfoStatusCheckIntervalMillis") final List<Long> trackingInfoStatusCheckIntervalMillis,
                           @Bind("trackingInfoStatusCheckUrl") final List<String> trackingInfoStatusCheckUrl,
                           @Bind("trackingInfoTrackingPipe") final List<String> trackingInfoTrackingPipe,
                           @Bind("trackingInfoTrackTo") final List<String> trackingInfoTrackTo,
                           @Bind("sourceInfo") final List<byte[]> sourceInfoList,
                           @Bind("correlationId") final List<String> correlationIdList);

}

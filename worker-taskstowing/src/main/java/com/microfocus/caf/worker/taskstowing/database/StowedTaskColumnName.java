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

public final class StowedTaskColumnName
{
    public static final String ID = "id";
    public static final String PARTITION_ID = "partition_id";
    public static final String JOB_ID = "job_id";
    public static final String TASK_CLASSIFIER = "task_classifier";
    public static final String TASK_API_VERSION = "task_api_version";
    public static final String TASK_DATA = "task_data";
    public static final String TASK_STATUS = "task_status";
    public static final String CONTEXT = "context";
    public static final String TO = "to";
    public static final String TRACKING_INFO = "tracking_info";
    public static final String SOURCE_INFO = "source_info";
    public static final String CORRELATION_ID = "correlation_id";
}

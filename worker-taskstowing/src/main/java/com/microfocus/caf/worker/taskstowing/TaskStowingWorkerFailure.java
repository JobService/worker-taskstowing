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

final class TaskStowingWorkerFailure
{
    static final String TRACKING_INFO_NOT_PRESENT
        = "Unable to stow task. Tracking info not present.";
    static final String JOB_TASK_ID_NOT_PRESENT
        = "Unable to stow task. Job task ID not present.";
    static final String FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID
        = "Unable to stow task. Failed to parse partition ID from job task id.";
    static final String FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID
        = "Unable to stow task. Failed to parse job ID from job task id.";
    static final String FAILED_TO_SERIALIZE_TASK
        = "Unable to stow task. Failed to serialize task.";
    static final String FAILED_TO_WRITE_TO_DATABASE
        = "Unable to stow task(s). Failed to write to database.";
    static final String UNKNOWN_ERROR
        = "Unable to stow task(s). An unknown error occured when processing one or more task(s).";

    private TaskStowingWorkerFailure()
    {
    }
}

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

import com.google.common.base.MoreObjects;
import com.hpe.caf.worker.document.model.Document;

import java.text.MessageFormat;

public enum TaskStowingWorkerFailure
{
    WORKER_TASK_DATA_NOT_PRESENT("TSW-001", "Unable to stow task. Worker task data not present."),
    TRACKING_INFO_NOT_PRESENT("TSW-002", "Unable to stow task. Tracking info not present."),
    JOB_TASK_ID_NOT_PRESENT("TSW-003", "Unable to stow task. Job task ID not present."),
    FAILED_TO_PARSE_PARTITION_ID_FROM_JOB_TASK_ID("TSW-004", "Unable to stow task. Failed to parse partition ID from job task id."),
    FAILED_TO_PARSE_JOB_ID_FROM_JOB_TASK_ID("TSW-005", "Unable to stow task. Failed to parse job ID from job task id."),
    FAILED_TO_SERIALIZE_TASK("TSW-006", "Unable to stow task. Failed to serialize task."),
    FAILED_TO_WRITE_TO_DATABASE("TSW-007", "Unable to stow task. Failed to write to database.");

    private final String failureId;
    private final String failureMsg;

    TaskStowingWorkerFailure(final String failureId, final String failureMsg)
    {
        this.failureId = failureId;
        this.failureMsg = failureMsg;
    }

    public String getFailureMsg()
    {
        return failureMsg;
    }

    public void addToDoc(final Document document)
    {
        document.addFailure(failureId, failureMsg);
    }

    public void addToDoc(final Document document, final String context, final Throwable cause)
    {
        document.getFailures().add(failureId, MessageFormat.format(failureMsg, context), cause);
    }

    public void addToDoc(final Document document, final Throwable cause)
    {
        document.getFailures().add(failureId, failureMsg, cause);
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
            .add("failureId", failureId)
            .add("failureMsg", failureMsg)
            .toString();
    }
}

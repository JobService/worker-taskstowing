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

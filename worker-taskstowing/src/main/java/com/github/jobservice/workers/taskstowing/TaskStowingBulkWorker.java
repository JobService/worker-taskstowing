/*
 * Copyright 2023 Open Text.
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

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTask;
import com.github.jobservice.workers.taskstowing.database.DatabaseClient;
import com.github.jobservice.workers.taskstowing.database.DatabaseExceptionChecker;
import com.hpe.caf.api.worker.TaskRejectedException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskStowingBulkWorker
{
    public static final String WORKER_IDENTIFIER = TaskStowingBulkWorker.class.getSimpleName();
    public static final int WORKER_API_VERSION = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStowingBulkWorker.class);

    private final WorkerTaskHolder workerTaskHolder;
    private final String errorQueue;
    private final Codec codec;
    private final DatabaseClient databaseClient;

    public TaskStowingBulkWorker(
        final WorkerTaskHolder workerTaskHolder,
        final String errorQueue,
        final Codec codec,
        final DatabaseClient databaseClient)
    {
        this.workerTaskHolder = workerTaskHolder;
        this.errorQueue = errorQueue;
        this.codec = codec;
        this.databaseClient = databaseClient;
    }

    public void processTasks()
    {
        LOGGER.info("Received request to stow {} task(s)", workerTaskHolder.getWorkerTaskList().size());

        if (anyListsEmptyOrNotSameSize(workerTaskHolder.getWorkerTaskList(),
                                       workerTaskHolder.getPartitionIdList(),
                                       workerTaskHolder.getJobIdList(),
                                       workerTaskHolder.getTaskClassifierList(),
                                       workerTaskHolder.getTaskApiVersionList(),
                                       workerTaskHolder.getTaskDataList(),
                                       workerTaskHolder.getTaskStatusList(),
                                       workerTaskHolder.getToList(),
                                       workerTaskHolder.getTrackingInfoJobTaskIdList(),
                                       workerTaskHolder.getTrackingInfoLastStatusCheckTimeList(),
                                       workerTaskHolder.getTrackingInfoStatusCheckIntervalMillisList(),
                                       workerTaskHolder.getTrackingInfoStatusCheckUrlList(),
                                       workerTaskHolder.getTrackingInfoTrackingPipeList(),
                                       workerTaskHolder.getTrackingInfoTrackToList(),
                                       workerTaskHolder.getSourceInfoList(),
                                       workerTaskHolder.getCorrelationIdList())) {
            // Should not happen normally, but want to make sure we don't go ahead with the database insertion if it does.
            LOGGER.error(TaskStowingWorkerFailure.UNKNOWN_ERROR);
            workerTaskHolder.getWorkerTaskList().forEach(workerTask -> {
                workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.UNKNOWN_ERROR));
            });
            return;
        }

        try {
            databaseClient.insertStowedTasks(
                workerTaskHolder.getPartitionIdList(),
                workerTaskHolder.getJobIdList(),
                workerTaskHolder.getTaskClassifierList(),
                workerTaskHolder.getTaskApiVersionList(),
                workerTaskHolder.getTaskDataList(),
                workerTaskHolder.getTaskStatusList(),
                workerTaskHolder.getToList(),
                workerTaskHolder.getTrackingInfoJobTaskIdList(),
                workerTaskHolder.getTrackingInfoLastStatusCheckTimeList(),
                workerTaskHolder.getTrackingInfoStatusCheckIntervalMillisList(),
                workerTaskHolder.getTrackingInfoStatusCheckUrlList(),
                workerTaskHolder.getTrackingInfoTrackingPipeList(),
                workerTaskHolder.getTrackingInfoTrackToList(),
                workerTaskHolder.getSourceInfoList(),
                workerTaskHolder.getCorrelationIdList());

            workerTaskHolder.getWorkerTaskList().forEach(workerTask -> {
                workerTask.setResponse(createSuccessResultNoOutputToQueue());
            });
        } catch (final Exception exception) {
            LOGGER.error(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception);
            if (DatabaseExceptionChecker.isTransientException(exception)) {
                workerTaskHolder.getWorkerTaskList().forEach(workerTask -> {
                    workerTask.setResponse(new TaskRejectedException(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception));
                });
            } else {
                workerTaskHolder.getWorkerTaskList().forEach(workerTask -> {
                    workerTask.setResponse(createFailureResult(TaskStowingWorkerFailure.FAILED_TO_WRITE_TO_DATABASE, exception));
                });
            }
        }
    }

    private WorkerResponse createSuccessResultNoOutputToQueue()
    {
        return WorkerResponseFactory.createSuccessResultNoOutputToQueue(WORKER_IDENTIFIER, WORKER_API_VERSION);
    }

    private WorkerResponse createFailureResult(final String failure)
    {
        return WorkerResponseFactory.createFailureResult(errorQueue, WORKER_IDENTIFIER, WORKER_API_VERSION, codec, failure);
    }

    private WorkerResponse createFailureResult(final String failure, final Throwable cause)
    {
        return WorkerResponseFactory.createFailureResult(errorQueue, WORKER_IDENTIFIER, WORKER_API_VERSION, codec, failure, cause);
    }

    private static boolean anyListsEmptyOrNotSameSize(
        final List<WorkerTask> validatedWorkerTasks,
        final List<String> partitionIdList,
        final List<String> jobIdList,
        final List<String> taskClassifierList,
        final List<Integer> taskApiVersionList,
        final List<byte[]> taskDataList,
        final List<String> taskStatusList,
        final List<String> toList,
        final List<String> trackingInfoJobTaskIdList,
        final List<Long> trackingInfoLastStatusCheckTimeList,
        final List<Long> trackingInfoStatusCheckIntervalMillisList,
        final List<String> trackingInfoStatusCheckUrlList,
        final List<String> trackingInfoTrackingPipeList,
        final List<String> trackingInfoTrackToList,
        final List<byte[]> sourceInfoList,
        final List<String> correlationIdList)
    {
        final List<List<? extends Object>> lists = new ArrayList<>();
        lists.add(validatedWorkerTasks);
        lists.add(partitionIdList);
        lists.add(jobIdList);
        lists.add(taskClassifierList);
        lists.add(taskApiVersionList);
        lists.add(taskDataList);
        lists.add(taskStatusList);
        lists.add(toList);
        lists.add(trackingInfoJobTaskIdList);
        lists.add(trackingInfoLastStatusCheckTimeList);
        lists.add(trackingInfoStatusCheckIntervalMillisList);
        lists.add(trackingInfoStatusCheckUrlList);
        lists.add(trackingInfoTrackingPipeList);
        lists.add(trackingInfoTrackToList);
        lists.add(sourceInfoList);
        lists.add(correlationIdList);

        final int firstListSize = validatedWorkerTasks.size();
        return firstListSize == 0
            ? true
            : lists.stream().anyMatch(list -> list.size() != firstListSize);
    }
}

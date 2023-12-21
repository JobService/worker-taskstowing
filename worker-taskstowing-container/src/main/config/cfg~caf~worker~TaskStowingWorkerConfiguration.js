/*
 * Copyright 2021-2024 Open Text.
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
({
    workerName: "worker-taskstowing",
    workerVersion: "${project.version}",
    outputQueue: getenv("CAF_WORKER_OUTPUT_QUEUE") || undefined,
    failureQueue: getenv("CAF_WORKER_FAILURE_QUEUE") ||
        (getenv("CAF_WORKER_BASE_QUEUE_NAME") || getenv("CAF_WORKER_NAME") || "worker") + "-err",
    threads: getenv("CAF_WORKER_THREADS") || 1,
    maxBatchTime: getenv("CAF_WORKER_MAX_BATCH_TIME") || 180000,
    maxBatchSize: getenv("CAF_WORKER_MAX_BATCH_SIZE") || 100,
    databaseHost: getenv("CAF_WORKER_TASKSTOWING_DATABASE_HOST") || "localhost",
    databasePort: getenv("CAF_WORKER_TASKSTOWING_DATABASE_PORT") || 5432,
    databaseName: getenv("CAF_WORKER_TASKSTOWING_DATABASE_NAME") || "jobservice",
    databaseTableName: getenv("CAF_WORKER_TASKSTOWING_DATABASE_TABLENAME") || "stowed_task",
    databaseUsername: getenv("CAF_WORKER_TASKSTOWING_DATABASE_USERNAME"),
    databasePassword: getenv("CAF_WORKER_TASKSTOWING_DATABASE_PASSWORD"),
    databaseAppName: getenv("CAF_WORKER_TASKSTOWING_DATABASE_APPNAME") || "worker-taskstowing",
    databaseMaximumPoolSize: getenv("CAF_WORKER_TASKSTOWING_DATABASE_MAXIMUM_POOL_SIZE") || 5
});

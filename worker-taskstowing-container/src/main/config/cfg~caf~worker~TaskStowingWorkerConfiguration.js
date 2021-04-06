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
({
    databaseHost: getenv("CAF_WORKER_TASKSTOWING_DATABASE_HOST") || "localhost",
    databasePort: getenv("CAF_WORKER_TASKSTOWING_DATABASE_PORT") || 5432,
    databaseName: getenv("CAF_WORKER_TASKSTOWING_DATABASE_NAME") || "jobservice",
    databaseTableName: getenv("CAF_WORKER_TASKSTOWING_DATABASE_TABLENAME") || "stowed_task",
    databaseUsername: getenv("CAF_WORKER_TASKSTOWING_DATABASE_USERNAME") || "postgres",
    databasePassword: getenv("CAF_WORKER_TASKSTOWING_DATABASE_PASSWORD") || "postgres",
    databaseAppName: getenv("CAF_WORKER_TASKSTOWING_DATABASE_APPNAME") || "worker_taskstowing",
    databaseMaximumPoolSize: getenv("CAF_WORKER_TASKSTOWING_DATABASE_MAXIMUM_POOL_SIZE") || 5
});

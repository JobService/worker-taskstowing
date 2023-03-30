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
package com.github.jobservice.workers.taskstowing.factory;

import com.hpe.caf.api.worker.WorkerConfiguration;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class TaskStowingWorkerConfiguration extends WorkerConfiguration
{
    @NotNull
    @Size(min = 1)
    private String outputQueue;

    @NotNull
    @Size(min = 1)
    private String failureQueue;

    @Min(1)
    @Max(20)
    private int threads;

    @NotNull
    private long maxBatchTime;

    @Min(1)
    private int maxBatchSize;

    @NotNull
    private String databaseHost;

    @Min(1)
    private int databasePort;

    @NotNull
    private String databaseName;

    @NotNull
    private String databaseTableName;

    @NotNull
    private String databaseUsername;

    @NotNull
    private String databasePassword;

    @NotNull
    private String databaseAppName;

    @Min(1)
    private int databaseMaximumPoolSize;

    public String getOutputQueue()
    {
        return outputQueue;
    }

    public void setOutputQueue(final String outputQueue)
    {
        this.outputQueue = outputQueue;
    }

    public String getFailureQueue()
    {
        return failureQueue;
    }

    public void setFailureQueue(final String failureQueue)
    {
        this.failureQueue = failureQueue;
    }

    public int getThreads()
    {
        return threads;
    }

    public void setThreads(final int threads)
    {
        this.threads = threads;
    }

    public long getMaxBatchTime()
    {
        return maxBatchTime;
    }

    public void setMaxBatchTime(final int maxBatchTime)
    {
        this.maxBatchTime = maxBatchTime;
    }

    public int getMaxBatchSize()
    {
        return maxBatchSize;
    }

    public void setMaxBatchSize(final int maxBatchSize)
    {
        this.maxBatchSize = maxBatchSize;
    }

    public String getDatabaseHost()
    {
        return databaseHost;
    }

    public void setDatabaseHost(final String databaseHost)
    {
        this.databaseHost = databaseHost;
    }

    public int getDatabasePort()
    {
        return databasePort;
    }

    public void setDatabasePort(final int databasePort)
    {
        this.databasePort = databasePort;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public void setDatabaseName(final String databaseName)
    {
        this.databaseName = databaseName;
    }

    public String getDatabaseTableName()
    {
        return databaseTableName;
    }

    public void setDatabaseTableName(final String databaseTableName)
    {
        this.databaseTableName = databaseTableName;
    }

    public String getDatabaseUsername()
    {
        return databaseUsername;
    }

    public void setDatabaseUsername(final String databaseUsername)
    {
        this.databaseUsername = databaseUsername;
    }

    public String getDatabasePassword()
    {
        return databasePassword;
    }

    public void setDatabasePassword(final String databasePassword)
    {
        this.databasePassword = databasePassword;
    }

    public String getDatabaseAppName()
    {
        return databaseAppName;
    }

    public void setDatabaseAppName(final String databaseAppName)
    {
        this.databaseAppName = databaseAppName;
    }

    public int getDatabaseMaximumPoolSize()
    {
        return databaseMaximumPoolSize;
    }

    public void setDatabaseMaximumPoolSize(final int databaseMaximumPoolSize)
    {
        this.databaseMaximumPoolSize = databaseMaximumPoolSize;
    }
}

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
package com.microfocus.caf.worker.taskstowing.factory;

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

    public void setDatabaseTableName(String databaseTableName)
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

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

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.extensibility.DocumentWorkerFactory;
import com.hpe.caf.worker.document.model.Application;
import com.microfocus.caf.worker.taskstowing.TaskStowingWorker;
import com.microfocus.caf.worker.taskstowing.database.DatabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskStowingWorkerFactory implements DocumentWorkerFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStowingWorkerFactory.class);

    @Override
    public DocumentWorker createDocumentWorker(final Application application)
    {
        final ConfigurationSource configurationSource = application.getService(ConfigurationSource.class);
        final TaskStowingWorkerConfiguration configuration;
        try {
            configuration = configurationSource.getConfiguration(TaskStowingWorkerConfiguration.class);
        } catch (final ConfigurationException e) {
            LOGGER.error("Failed to load Task Stowing Worker configuration", e);
            return null;
        }
        return new TaskStowingWorker(new DatabaseClient(configuration));
    }
}

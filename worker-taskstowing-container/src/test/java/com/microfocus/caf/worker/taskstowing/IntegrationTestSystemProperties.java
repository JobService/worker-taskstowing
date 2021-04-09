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

import com.google.common.base.Strings;

final class IntegrationTestSystemProperties
{
    public final static String DOCKER_HOST_ADDRESS = checkNotNullOrEmpty("docker.host.address");
    public final static String DATABASE_PORT = checkNotNullOrEmpty("database.port");
    public final static String DATABASE_NAME = checkNotNullOrEmpty("database.name");
    public final static String DATABASE_USERANME = checkNotNullOrEmpty("database.username");
    public final static String DATABASE_PASSWORD = checkNotNullOrEmpty("database.password");
    public final static String DATABASE_TABLE_NAME  = checkNotNullOrEmpty("database.tablename");
    public static final String MOCK_JOB_SERVICE_PORT = checkNotNullOrEmpty("mock.job.service.port");
    public static final String RABBITMQ_NODE_PORT = checkNotNullOrEmpty("rabbitmq.node.port");
    public static final String RABBITMQ_CTRL_PORT = checkNotNullOrEmpty("rabbitmq.ctrl.port");

    private static String checkNotNullOrEmpty(final String systemPropertyKey)
    {
        final String systemProperty = System.getProperty(systemPropertyKey);
        if (Strings.isNullOrEmpty(systemProperty)) {
            throw new RuntimeException("System property should not be null or empty: " + systemPropertyKey);
        }
        return systemProperty;
    }

    private IntegrationTestSystemProperties()
    {
    }
}

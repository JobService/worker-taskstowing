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

import com.google.common.base.Strings;

final class IntegrationTestSystemProperties
{
    public final static String DOCKER_HOST_ADDRESS = checkNotNullOrEmpty("docker.host.address");
    public final static String DATABASE_PORT = checkNotNullOrEmpty("database.port");
    public final static String DATABASE_NAME = checkNotNullOrEmpty("database.name");
    public final static String DATABASE_USERANME = checkNotNullOrEmpty("database.username");
    public final static String DATABASE_PASSWORD = checkNotNullOrEmpty("database.password");
    public final static String DATABASE_TABLE_NAME  = checkNotNullOrEmpty("database.tablename");
    public static final String MOCK_SERVICE_PORT = checkNotNullOrEmpty("mock.service.port");
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

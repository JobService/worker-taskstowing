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
package com.github.jobservice.workers.taskstowing.database;

import com.github.jobservice.workers.taskstowing.factory.TaskStowingWorkerConfiguration;
import com.zaxxer.hikari.HikariDataSource;

final class HikariDataSourceFactory
{
    public static HikariDataSource createHikariDataSource(final TaskStowingWorkerConfiguration configuration)
    {
        final HikariDataSource hikariDataSource = new HikariDataSource();

        final String jdbcUrl = String.format(
            "jdbc:postgresql://%s:%s/%s?ApplicationName=%s",
            configuration.getDatabaseHost(),
            configuration.getDatabasePort(),
            configuration.getDatabaseName(),
            configuration.getDatabaseAppName());
        hikariDataSource.setJdbcUrl(jdbcUrl);
        hikariDataSource.setUsername(configuration.getDatabaseUsername());
        hikariDataSource.setPassword(configuration.getDatabasePassword());
        hikariDataSource.setMaximumPoolSize(configuration.getDatabaseMaximumPoolSize());

        return hikariDataSource;
    }

    private HikariDataSourceFactory()
    {
    }
}

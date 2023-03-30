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
package com.github.jobservice.workers.taskstowing.database;

import com.github.jobservice.workers.taskstowing.factory.TaskStowingWorkerConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.PGProperty;

final class HikariDataSourceFactory
{
    public static HikariDataSource createHikariDataSource(final TaskStowingWorkerConfiguration configuration)
    {
        final HikariDataSource hikariDataSource = new HikariDataSource();

        hikariDataSource.setJdbcUrl("jdbc:postgresql:");
        hikariDataSource.addDataSourceProperty(PGProperty.PG_HOST.getName(), configuration.getDatabaseHost());
        hikariDataSource.addDataSourceProperty(PGProperty.PG_PORT.getName(), configuration.getDatabasePort());
        hikariDataSource.addDataSourceProperty(PGProperty.PG_DBNAME.getName(), configuration.getDatabaseName());
        hikariDataSource.addDataSourceProperty(PGProperty.APPLICATION_NAME.getName(), configuration.getDatabaseAppName());
        hikariDataSource.addDataSourceProperty(PGProperty.USER.getName(), configuration.getDatabaseUsername());
        hikariDataSource.addDataSourceProperty(PGProperty.PASSWORD.getName(), configuration.getDatabasePassword());
        hikariDataSource.setMaximumPoolSize(configuration.getDatabaseMaximumPoolSize());

        return hikariDataSource;
    }

    private HikariDataSourceFactory()
    {
    }
}

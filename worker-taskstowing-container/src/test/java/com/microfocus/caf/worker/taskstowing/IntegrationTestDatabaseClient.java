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
import java.util.List;
import org.jdbi.v3.core.Jdbi;
import org.testng.Assert;

final class IntegrationTestDatabaseClient
{
    private final Jdbi jdbi;
    private final String databaseTableName;

    public IntegrationTestDatabaseClient()
    {
        final String dockerHostAddress = checkNotNullOrEmpty("docker.host.address");
        final String databasePort = checkNotNullOrEmpty("database.port");
        final String databaseName = checkNotNullOrEmpty("database.name");
        final String databaseUsername = checkNotNullOrEmpty("database.username");
        final String databasePassword = checkNotNullOrEmpty("database.password");
        this.databaseTableName = checkNotNullOrEmpty("database.tablename");

        final String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", dockerHostAddress, databasePort, databaseName);
        this.jdbi = Jdbi.create(jdbcUrl, databaseUsername, databasePassword);
    }

    public List<StowedTaskRow> waitUntilStowedTaskTableContains(final int expectedNumberOfStowedTasks, final int timeoutMillis)
        throws InterruptedException, Exception
    {
        final long deadline = System.currentTimeMillis() + timeoutMillis;
        List<StowedTaskRow> stowedTasks = getStowedTasks();
        while (stowedTasks.size() != expectedNumberOfStowedTasks) {
            Thread.sleep(500);
            long remaining = deadline - System.currentTimeMillis();
            if (remaining < 0) {
                Assert.fail("Timed out out after " + timeoutMillis + " milliseconds waiting on " + databaseTableName + " to contain "
                    + expectedNumberOfStowedTasks + " stowed tasks");
            }
            stowedTasks = getStowedTasks();
        }
        return stowedTasks;
    }

    public List<StowedTaskRow> getStowedTasks() throws Exception
    {
        return jdbi.withHandle(handle -> {
            return handle.createQuery("SELECT * FROM " + databaseTableName)
                .map(new StowedTaskRowMapper())
                .list();
        });
    }

    public void deleteStowedTasks() throws Exception
    {
        jdbi.useHandle(handle -> {
            handle.execute("DELETE FROM " + databaseTableName);
        });
    }

    private static String checkNotNullOrEmpty(final String systemPropertyKey)
    {
        final String systemProperty = System.getProperty(systemPropertyKey);
        if (Strings.isNullOrEmpty(systemProperty)) {
            throw new RuntimeException("System property should not be null or empty: " + systemPropertyKey);
        }
        return systemProperty;
    }
}

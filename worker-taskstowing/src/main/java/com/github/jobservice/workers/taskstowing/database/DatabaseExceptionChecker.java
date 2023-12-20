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
package com.github.jobservice.workers.taskstowing.database;

import com.google.common.collect.Lists;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.List;
import org.jdbi.v3.core.JdbiException;

public final class DatabaseExceptionChecker
{
    // Postgres error codes: https://www.postgresql.org/docs/current/errcodes-appendix.html
    // Transient error codes: https://www.postgresql-archive.org/Determine-if-an-error-is-transient-by-its-error-code-td5950594.html

    private static final String CONNECTION_EXCEPTION_ERROR_CLASS = "08";
    private static final String INSUFFICIENT_RESOURCES_ERROR_CLASS = "53";
    private static final String OPERATOR_FAILURE_ERROR_CLASS = "57";
    private static final List<String> TRANSIENT_ERROR_CLASSES = Lists.newArrayList(
        CONNECTION_EXCEPTION_ERROR_CLASS,
        INSUFFICIENT_RESOURCES_ERROR_CLASS,
        OPERATOR_FAILURE_ERROR_CLASS);

    private static final String READ_ONLY_SQL_TRANSACTION_ERROR_CODE = "25006";
    private static final String LOCK_NOT_AVAILABLE_ERROR_CODE = "55P03";
    private static final String OBJECT_IN_USE_ERROR_CODE = "55006";
    private static final String SYSTEM_ERROR_CODE = "58000";
    private static final String IO_ERROR_CODE = "58030";
    private static final List<String> TRANSIENT_ERROR_CODES = Lists.newArrayList(
        READ_ONLY_SQL_TRANSACTION_ERROR_CODE,
        LOCK_NOT_AVAILABLE_ERROR_CODE,
        OBJECT_IN_USE_ERROR_CODE,
        SYSTEM_ERROR_CODE,
        IO_ERROR_CODE);

    public static boolean isTransientException(final Exception exception)
    {
        final Throwable unwrappedException = (exception instanceof JdbiException) ? exception.getCause() : exception;

        if (unwrappedException instanceof SQLTransientException) {
            return true;
        }

        if (!(unwrappedException instanceof SQLException)) {
            return false;
        }

        return isSqlStatePrefixedWith(((SQLException) unwrappedException), TRANSIENT_ERROR_CLASSES)
            || isSqlStateEqualTo(((SQLException) unwrappedException), TRANSIENT_ERROR_CODES);
    }

    private static boolean isSqlStatePrefixedWith(final SQLException exception, final List<String> errorClasses)
    {
        final String sqlState = exception.getSQLState();
        if (sqlState == null || sqlState.length() != 5) {
            return false;
        }

        return errorClasses.stream().anyMatch((errorClass) -> (sqlState.startsWith(errorClass)));
    }

    private static boolean isSqlStateEqualTo(final SQLException exception, final List<String> errorCodes)
    {
        final String sqlState = exception.getSQLState();
        if (sqlState == null || sqlState.length() != 5) {
            return false;
        }

        return errorCodes.stream().anyMatch((errorCode) -> (sqlState.equals(errorCode)));
    }

    private DatabaseExceptionChecker()
    {
    }
}

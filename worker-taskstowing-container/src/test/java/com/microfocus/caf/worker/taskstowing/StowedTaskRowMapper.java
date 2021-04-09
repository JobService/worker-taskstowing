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

import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import static com.microfocus.caf.worker.taskstowing.database.StowedTaskColumnName.*;

final class StowedTaskRowMapper implements RowMapper<StowedTaskRow>
{
    @Override
    public StowedTaskRow map(final ResultSet resultSet, final StatementContext statementContext) throws SQLException
    {
        return new StowedTaskRow(
            resultSet.getLong(ID),
            resultSet.getString(PARTITION_ID),
            resultSet.getString(JOB_ID),
            resultSet.getString(TASK_CLASSIFIER),
            resultSet.getInt(TASK_API_VERSION),
            resultSet.getBytes(TASK_DATA),
            resultSet.getString(TASK_STATUS),
            resultSet.getBytes(CONTEXT),
            resultSet.getString(TO),
            resultSet.getBytes(TRACKING_INFO),
            resultSet.getBytes(SOURCE_INFO),
            resultSet.getString(CORRELATION_ID));
    }
}

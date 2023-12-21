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
package com.github.jobservice.workers.taskstowing;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import static com.github.jobservice.workers.taskstowing.database.StowedTaskColumnName.*;

final class StowedTaskRowMapper implements RowMapper<StowedTaskRow>
{
    @Override
    public StowedTaskRow map(final ResultSet resultSet, final StatementContext statementContext) throws SQLException
    {
        return new StowedTaskRow(
            resultSet.getString(PARTITION_ID),
            resultSet.getString(JOB_ID),
            resultSet.getString(TASK_CLASSIFIER),
            resultSet.getInt(TASK_API_VERSION),
            resultSet.getBytes(TASK_DATA),
            resultSet.getString(TASK_STATUS),
            resultSet.getString(TO),
            resultSet.getString(TRACKING_INFO_JOB_TASK_ID),
            resultSet.getLong(TRACKING_INFO_LAST_STATUS_CHECK_TIME),
            resultSet.getLong(TRACKING_INFO_STATUS_CHECK_INTERVAL_MILLIS),
            resultSet.getString(TRACKING_INFO_STATUS_CHECK_URL),
            resultSet.getString(TRACKING_INFO_TRACKING_PIPE),
            resultSet.getString(TRACKING_INFO_TRACK_TO),
            resultSet.getBytes(SOURCE_INFO),
            resultSet.getString(CORRELATION_ID));
    }
}

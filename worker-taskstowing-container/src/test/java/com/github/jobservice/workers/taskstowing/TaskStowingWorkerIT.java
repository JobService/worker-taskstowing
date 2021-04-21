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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskSourceInfo;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.jobservice.workers.taskstowing.IntegrationTestSystemProperties.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import com.hpe.caf.api.worker.JobStatus;
import java.time.LocalDate;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.BeforeClass;

public class TaskStowingWorkerIT
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStowingWorkerIT.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String DATA_PROCESSING_ELASTICQUERY_IN_QUEUE = "dataprocessing-elasticquery-in";
    private static final String PAUSED_JOB_STATUS_CHECK_URL_PATH = "/partitions/tenant-acme/jobs/job1/status";
    private static final String PAUSED_JOB_STATUS_CHECK_URL
        = String.format("http://%s:%s%s", DOCKER_HOST_ADDRESS, MOCK_SERVICE_PORT, PAUSED_JOB_STATUS_CHECK_URL_PATH);
    private static final String ACTIVE_JOB_STATUS_CHECK_URL_PATH = "/partitions/tenant-acme/jobs/job2/status";
    private static final String ACTIVE_JOB_STATUS_CHECK_URL
        = String.format("http://%s:%s%s", DOCKER_HOST_ADDRESS, MOCK_SERVICE_PORT, ACTIVE_JOB_STATUS_CHECK_URL_PATH);
    private static final Date ONE_DAY_AGO = java.sql.Date.valueOf(LocalDate.now().minusDays(1));
    private static final long TWO_MINUTES_IN_MILLIS = 120000L;

    private final IntegrationTestDatabaseClient integrationTestDatabaseClient = new IntegrationTestDatabaseClient();
    private QueueServices queueServices;

    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException
    {
        // Instruct the mock service to return a "Paused" status whenever the worker calls the specified statusCheckUrl.
        MockServiceExpectationAdder.addExpectation(
            new MockServiceExpection("GET", PAUSED_JOB_STATUS_CHECK_URL_PATH, 200, JobStatus.Paused.name()));

        // Instruct the mock service to return an "Active" status whenever the worker calls the specified statusCheckUrl.
        MockServiceExpectationAdder.addExpectation(
            new MockServiceExpection("GET", ACTIVE_JOB_STATUS_CHECK_URL_PATH, 200, JobStatus.Active.name()));
    }

    @BeforeMethod
    public void setUpTest(Method method) throws Exception
    {
        System.out.println("RORY " + PAUSED_JOB_STATUS_CHECK_URL);
        LOGGER.info("Starting: {}", method.getName());
        queueServices = new QueueServices();
    }

    @AfterMethod
    public void cleanupTest(Method method) throws Exception
    {
        queueServices.close();
        integrationTestDatabaseClient.deleteStowedTasks();
        LOGGER.info("End of: {}", method.getName());
    }

    @Test // Test stowing a task from a DocumentWorker, as opposed to a Worker
    public void testStowDocumentWorkerTask() throws IOException, InterruptedException, Exception
    {
        // Given a task message
        final TrackingInfo trackingInfo = new TrackingInfo(
            "tenant-acme:job1",
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            PAUSED_JOB_STATUS_CHECK_URL,
            "dataprocessing-jobtracking-in",
            null);

        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        documentWorkerDocumentTask.document = createSampleDocument();

        // The context key must be set to the servicePath of the worker (which is caf/worker), otherwise the context won't be passed to
        // the worker:
        // https://github.com/WorkerFramework/worker-framework/blob/develop/worker-core/src/main/java/com/hpe/caf/worker/core/WorkerTaskImpl.java#L124
        final Map<String, byte[]> context =  new HashMap<>();
        final Map<String, String> contextContents = new HashMap<>();
        contextContents.put("a-context-key", "a-context-value");
        context.put("caf/worker", OBJECT_MAPPER.writeValueAsBytes(contextContents));

        final TaskMessage taskMessage = new TaskMessage(
            UUID.randomUUID().toString(),
            "DocumentWorkerTask",
            2,
            OBJECT_MAPPER.writeValueAsBytes(documentWorkerDocumentTask),
            TaskStatus.NEW_TASK,
            context,
            DATA_PROCESSING_ELASTICQUERY_IN_QUEUE,
            trackingInfo,
            new TaskSourceInfo("agent1", "1.0"),
            "123");

        // When the task message is sent to the worker
        queueServices.startListening();
        queueServices.sendTaskMessage(taskMessage);

        // Then the task should have been stowed in the database
        final List<StowedTaskRow> stowedTaskRows = integrationTestDatabaseClient.waitUntilStowedTaskTableContains(1, 30000);
        final StowedTaskRow stowedTaskRow = stowedTaskRows.get(0);

        // Single value fields
        assertEquals("Unexpected value in database for partition_id", "tenant-acme", stowedTaskRow.getPartitionId());
        assertEquals("Unexpected value in database for job_id", "job1", stowedTaskRow.getJobId());
        assertEquals("Unexpected value in database for task_classifier", "DocumentWorkerTask", stowedTaskRow.getTaskClassifier());
        assertEquals("Unexpected value in database for task_api_version", 2, stowedTaskRow.getTaskApiVersion());
        assertEquals("Unexpected value in database for task_status", "NEW_TASK", stowedTaskRow.getTaskStatus());
        assertEquals("Unexpected value in database for to", DATA_PROCESSING_ELASTICQUERY_IN_QUEUE, stowedTaskRow.getTo());
        assertEquals("Unexpected value in database for correlation_id", "123", stowedTaskRow.getCorrelationId());

        // task_data
        final DocumentWorkerDocumentTask taskDataFromDatabase
            = OBJECT_MAPPER.readValue(stowedTaskRow.getTaskData(), DocumentWorkerDocumentTask.class);
        assertNotNull("task_data value in database should not be null", taskDataFromDatabase);
        assertEquals("Unexpected value in database for task_data.document.reference",
                     "1", taskDataFromDatabase.document.reference);
        assertEquals("Unexpected value in database for task_data.document.fields.TENANT",
                     "acme", taskDataFromDatabase.document.fields.get("TENANT").get(0).data);

        // context
        final Map<String, byte[]> contextFromDatabase = OBJECT_MAPPER.readValue(stowedTaskRow.getContext(), Map.class);
        assertEquals("Unexpected size of context in database", 1, contextFromDatabase.size());
        assertEquals("Unexpected value of a-context-key in database", "a-context-value", contextFromDatabase.get("a-context-key"));

        // tracking_info
        assertEquals("Unexpected value in database for tracking_info.jobTaskId",
                     "tenant-acme:job1", stowedTaskRow.getTrackingInfoJobTaskId());
        assertNotNull("tracking_info.lastStatusCheckTime value in database should not be null",
                      stowedTaskRow.getTrackingInfoLastStatusCheckTime());
        assertEquals("Unexpected value in database for tracking_info.statusCheckIntervalMillis",
                     TWO_MINUTES_IN_MILLIS, stowedTaskRow.getTrackingInfoStatusCheckIntervalMillis().longValue());
        assertEquals("Unexpected value in database for tracking_info.statusCheckUrl",
                     PAUSED_JOB_STATUS_CHECK_URL, stowedTaskRow.getTrackingInfoStatusCheckUrl());
        assertEquals("Unexpected value in database for tracking_info.trackingPipe",
                     "dataprocessing-jobtracking-in", stowedTaskRow.getTrackingInfoTrackingPipe());
        assertNull("tracking_info.trackTo value in database should be null", stowedTaskRow.getTrackingInfoTrackTo());

        // source_info
        final TaskSourceInfo taskSourceInfoFromDatabase = OBJECT_MAPPER.readValue(stowedTaskRow.getSourceInfo(), TaskSourceInfo.class);
        assertNotNull("source_info value in database should not be null", taskSourceInfoFromDatabase);
        assertEquals("Unexpected value in database for source_info.name",
                     "agent1", taskSourceInfoFromDatabase.getName());
        assertEquals("Unexpected value in database for source_info.version",
                     "1.0", taskSourceInfoFromDatabase.getVersion());
    }

    @Test // Test stowing a task from a 'normal' worker, i.e. a Worker rather than a DocumentWorker
    public void testStowWorkerTask() throws IOException, InterruptedException, Exception
    {
        // Given a task message
        final TrackingInfo trackingInfo = new TrackingInfo(
            "tenant-acme:job1",
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            PAUSED_JOB_STATUS_CHECK_URL,
            "dataprocessing-jobtracking-in",
            null);

        final Map<String, String> taskData = new HashMap<>();
        taskData.put("someTaskDataKey", "someTaskDataValue");

        final TaskMessage taskMessage = new TaskMessage(
            UUID.randomUUID().toString(),
            "DocumentWorkerTask",
            2,
            OBJECT_MAPPER.writeValueAsBytes(taskData),
            TaskStatus.NEW_TASK,
            Collections.<String, byte[]>emptyMap(),
            DATA_PROCESSING_ELASTICQUERY_IN_QUEUE,
            trackingInfo,
            new TaskSourceInfo("agent1", "1.0"),
            "123");

        // When the task message is sent to the worker
        queueServices.startListening();
        queueServices.sendTaskMessage(taskMessage);

        // Then the task should have been stowed in the database
        final List<StowedTaskRow> stowedTaskRows = integrationTestDatabaseClient.waitUntilStowedTaskTableContains(1, 30000);
        final StowedTaskRow stowedTaskRow = stowedTaskRows.get(0);

        // Single value fields
        assertEquals("Unexpected value in database for partition_id", "tenant-acme", stowedTaskRow.getPartitionId());
        assertEquals("Unexpected value in database for job_id", "job1", stowedTaskRow.getJobId());
        assertEquals("Unexpected value in database for task_classifier", "DocumentWorkerTask", stowedTaskRow.getTaskClassifier());
        assertEquals("Unexpected value in database for task_api_version", 2, stowedTaskRow.getTaskApiVersion());
        assertEquals("Unexpected value in database for task_status", "NEW_TASK", stowedTaskRow.getTaskStatus());
        assertEquals("Unexpected value in database for to", DATA_PROCESSING_ELASTICQUERY_IN_QUEUE, stowedTaskRow.getTo());
        assertEquals("Unexpected value in database for correlation_id", "123", stowedTaskRow.getCorrelationId());

        // task_data
        final Map<String, String> taskDataFromDatabase
            = OBJECT_MAPPER.readValue(stowedTaskRow.getTaskData(), Map.class);
        assertNotNull("task_data value in database should not be null", taskDataFromDatabase);
        assertTrue("task_data value in database should be an object containing the key: someTaskDataKey",
                   taskDataFromDatabase.containsKey("someTaskDataKey"));
        assertEquals("Unexpected value in database for task_data.someTaskDataKey",
                     "someTaskDataValue", taskDataFromDatabase.get("someTaskDataKey"));

        // context
        final Map<String, byte[]> contextFromDatabase = OBJECT_MAPPER.readValue(stowedTaskRow.getContext(), Map.class);
        assertEquals("Unexpected value in database for context", 0, contextFromDatabase.size());

        // tracking_info
        assertEquals("Unexpected value in database for tracking_info.jobTaskId",
                     "tenant-acme:job1", stowedTaskRow.getTrackingInfoJobTaskId());
        assertNotNull("tracking_info.lastStatusCheckTime value in database should not be null",
                      stowedTaskRow.getTrackingInfoLastStatusCheckTime());
        assertEquals("Unexpected value in database for tracking_info.statusCheckIntervalMillis",
                     TWO_MINUTES_IN_MILLIS, stowedTaskRow.getTrackingInfoStatusCheckIntervalMillis().longValue());
        assertEquals("Unexpected value in database for tracking_info.statusCheckUrl",
                     PAUSED_JOB_STATUS_CHECK_URL, stowedTaskRow.getTrackingInfoStatusCheckUrl());
        assertEquals("Unexpected value in database for tracking_info.trackingPipe",
                     "dataprocessing-jobtracking-in", stowedTaskRow.getTrackingInfoTrackingPipe());
        assertNull("tracking_info.trackTo value in database should be null", stowedTaskRow.getTrackingInfoTrackTo());

        // source_info
        final TaskSourceInfo taskSourceInfoFromDatabase = OBJECT_MAPPER.readValue(stowedTaskRow.getSourceInfo(), TaskSourceInfo.class);
        assertNotNull("source_info value in database should not be null", taskSourceInfoFromDatabase);
        assertEquals("Unexpected value in database for source_info.name",
                     "agent1", taskSourceInfoFromDatabase.getName());
        assertEquals("Unexpected value in database for source_info.version",
                     "1.0", taskSourceInfoFromDatabase.getVersion());
    }

    @Test // When this worker recieves a task for a job that is active, it should forward the task rather than stowing it
    public void testForwardTaskWhenJobIsActive() throws IOException, InterruptedException, Exception
    {
        // Given a task message with a statusCheckUrl that will return an "Active" status
        final TrackingInfo trackingInfo = new TrackingInfo(
            "tenant-acme:job2",
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            ACTIVE_JOB_STATUS_CHECK_URL,
            "dataprocessing-jobtracking-in",
            null);

        final Map<String, String> taskData = new HashMap<>();
        taskData.put("someTaskDataKey", "someTaskDataValue");

        final TaskMessage taskMessage = new TaskMessage(
            UUID.randomUUID().toString(),
            "DocumentWorkerTask",
            2,
            OBJECT_MAPPER.writeValueAsBytes(taskData),
            TaskStatus.NEW_TASK,
            Collections.<String, byte[]>emptyMap(),
            DATA_PROCESSING_ELASTICQUERY_IN_QUEUE,
            trackingInfo,
            new TaskSourceInfo("agent1", "1.0"),
            "123");

        // When the task message is sent to the worker
        queueServices.startListening();
        queueServices.sendTaskMessage(taskMessage);

        // Then the worker should have forwarded the message to the worker whom the task is intended for (the "to" field in the task msg)
        queueServices.waitForForwardQueueMessages(1, 30000);
        assertEquals("Expected 1 message to have been forwarded to the worker pointed to by the 'to' field in the task message", 1,
                     queueServices.getForwardQueueMessages().size());

        // and the worker should NOT have stowed the task message in the database
        final List<StowedTaskRow> stowedTaskRows = integrationTestDatabaseClient.getStowedTasks();
        assertEquals("Expected 0 rows to have been written to the database", 0, stowedTaskRows.size());
    }

    @Test
    public void testTaskWithoutJobTaskIdIsNotStowed() throws IOException, InterruptedException, Exception
    {
        // Given a task message without a job task ID
        final TrackingInfo trackingInfo = new TrackingInfo(
            "",
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            PAUSED_JOB_STATUS_CHECK_URL,
            "dataprocessing-jobtracking-in",
            null);

        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        documentWorkerDocumentTask.document = createSampleDocument();

        final TaskMessage taskMessage = new TaskMessage(
            UUID.randomUUID().toString(),
            "DocumentWorkerTask",
            2,
            OBJECT_MAPPER.writeValueAsBytes(documentWorkerDocumentTask),
            TaskStatus.NEW_TASK,
            Collections.<String, byte[]>emptyMap(),
            DATA_PROCESSING_ELASTICQUERY_IN_QUEUE,
            trackingInfo,
            null,
            null);

        // When the task message is sent to the worker
        queueServices.startListening();
        queueServices.sendTaskMessage(taskMessage);

        // Then the worker should have sent the message to it's error queue
        queueServices.waitForErrorQueueMessages(1, 30000);
        assertEquals("Expected 1 message to have been sent to the worker's error queue", 1,
                     queueServices.getErrorQueueMessages().size());

        // and the worker should NOT have stowed the task message in the database
        final List<StowedTaskRow> stowedTaskRows = integrationTestDatabaseClient.getStowedTasks();
        assertEquals("Expected 0 rows to have been written to the database", 0, stowedTaskRows.size());
    }

    @Test
    public void testTaskWithEmptyPartitionIdIsNotStowed() throws IOException, InterruptedException, Exception
    {
        // Given a task message with an invalid job task ID
        final TrackingInfo trackingInfo = new TrackingInfo(
            ":", // Invalid as the worker is unable to parse a partition ID from this
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            PAUSED_JOB_STATUS_CHECK_URL,
            "dataprocessing-jobtracking-in",
            null);

        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        documentWorkerDocumentTask.document = createSampleDocument();

        final TaskMessage taskMessage = new TaskMessage(
            UUID.randomUUID().toString(),
            "DocumentWorkerTask",
            2,
            OBJECT_MAPPER.writeValueAsBytes(documentWorkerDocumentTask),
            TaskStatus.NEW_TASK,
            Collections.<String, byte[]>emptyMap(),
            DATA_PROCESSING_ELASTICQUERY_IN_QUEUE,
            trackingInfo,
            null,
            null);

        // When the task message is sent to the worker
        queueServices.startListening();
        queueServices.sendTaskMessage(taskMessage);

        // Then the worker should have sent the message to it's error queue
        queueServices.waitForErrorQueueMessages(1, 30000);
        assertEquals("Expected 1 message to have been sent to the worker's error queue", 1,
                     queueServices.getErrorQueueMessages().size());

        // and the worker should NOT have stowed the task message in the database
        final List<StowedTaskRow> stowedTaskRows = integrationTestDatabaseClient.getStowedTasks();
        assertEquals("Expected 0 rows to have been written to the database", 0, stowedTaskRows.size());
    }

    @Test
    public void testTaskWithEmptyJobIdIsNotStowed() throws IOException, InterruptedException, Exception
    {
        // Given a task message with an invalid job task ID
        final TrackingInfo trackingInfo = new TrackingInfo(
            ".abc.", // Invalid as the worker is unable to parse a job ID from this
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            PAUSED_JOB_STATUS_CHECK_URL,
            "dataprocessing-jobtracking-in",
            null);

        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        documentWorkerDocumentTask.document = createSampleDocument();

        final TaskMessage taskMessage = new TaskMessage(
            UUID.randomUUID().toString(),
            "DocumentWorkerTask",
            2,
            OBJECT_MAPPER.writeValueAsBytes(documentWorkerDocumentTask),
            TaskStatus.NEW_TASK,
            Collections.<String, byte[]>emptyMap(),
            DATA_PROCESSING_ELASTICQUERY_IN_QUEUE,
            trackingInfo,
            null,
            null);

        // When the task message is sent to the worker
        queueServices.startListening();
        queueServices.sendTaskMessage(taskMessage);

        // Then the worker should have sent the message to it's error queue
        queueServices.waitForErrorQueueMessages(1, 30000);
        assertEquals("Expected 1 message to have been sent to the worker's error queue", 1,
                     queueServices.getErrorQueueMessages().size());

        // and the worker should NOT have stowed the task message in the database
        final List<StowedTaskRow> stowedTaskRows = integrationTestDatabaseClient.getStowedTasks();
        assertEquals("Expected 0 rows to have been written to the database", 0, stowedTaskRows.size());
    }

    private static DocumentWorkerDocument createSampleDocument()
    {
        final DocumentWorkerDocument documentWorkerDocument = new DocumentWorkerDocument();
        documentWorkerDocument.reference = "1";
        final Map<String, List<DocumentWorkerFieldValue>> documentWorkerDocumentFields = new HashMap<>();
        final DocumentWorkerFieldValue tenantFieldValue = new DocumentWorkerFieldValue();
        tenantFieldValue.encoding = DocumentWorkerFieldEncoding.utf8;
        tenantFieldValue.data = "acme";
        documentWorkerDocumentFields.put("TENANT", ImmutableList.of(tenantFieldValue));
        documentWorkerDocument.fields = documentWorkerDocumentFields;
        return documentWorkerDocument;
    }
}

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
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microfocus.caf.worker.taskstowing.IntegrationTestUtil.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
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
    private static final String DOCKER_HOST_ADDRESS = checkNotNullOrEmpty("docker.host.address");
    private static final String MOCK_JOB_SERVICE_PORT = checkNotNullOrEmpty("mock.job.service.port");
    private static final String MOCK_JOB_SERVICE_STATUS_CHECK_URL = String.format("http://%s:%s/partitions/tenant-acme/jobs/job1/status",
                                                                                  DOCKER_HOST_ADDRESS, MOCK_JOB_SERVICE_PORT);
    private static final Date ONE_DAY_AGO = java.sql.Date.valueOf(LocalDate.now().minusDays(1));
    private static final long TWO_MINUTES_IN_MILLIS = 120000L;

    private final IntegrationTestDatabaseClient integrationTestDatabaseClient = new IntegrationTestDatabaseClient();
    private QueueServices queueServices;

    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException
    {
        // Instruct the mock job service to return a "Paused" status whenever the worker calls the statusCheckUrl during tests.
        mockStatusCheckUrlResponse();
    }

    private static void mockStatusCheckUrlResponse() throws IOException, InterruptedException
    {
        final String statusCheckUrlExpectationUrl = String.format("http://%s:%s/expectation", DOCKER_HOST_ADDRESS, MOCK_JOB_SERVICE_PORT);
        final String statusCheckUrlExpectationJson = loadStatusCheckUrlExpectationJson();
        final RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), statusCheckUrlExpectationJson);
        final Request request = new Request.Builder().url(statusCheckUrlExpectationUrl).put(body).build();
        try (final Response response = new OkHttpClient().newCall(request).execute()) {
            if (response.code() != 201) {
                throw new RuntimeException(
                    "Unexpected response code returned from mock server PUT request. Expected 201 but got " + response.code());
            }
        }
    }

    private static String loadStatusCheckUrlExpectationJson() throws IOException, InterruptedException
    {
        try (final InputStream inputStream = TaskStowingWorkerIT.class.getResourceAsStream("/status-check-url-expectation.json")) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    @BeforeMethod
    public void setUpTest(Method method) throws Exception
    {
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

    @Test
    public void testStowDocumentWorkerTask() throws IOException, InterruptedException, Exception
    {
        // Given a task message
        final TrackingInfo trackingInfo = new TrackingInfo(
            "tenant-acme:job1",
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            MOCK_JOB_SERVICE_STATUS_CHECK_URL,
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
            "dataprocessing-elasticquery-in",
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
        assertEquals("Unexpected value in database for to", "dataprocessing-elasticquery-in", stowedTaskRow.getTo());
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
        assertEquals("Unexpected value in database for context", 0, contextFromDatabase.size());

        // tracking_info
        final TrackingInfo trackingInfoFromDatabase = OBJECT_MAPPER.readValue(stowedTaskRow.getTrackingInfo(), TrackingInfo.class);
        assertNotNull("tracking_info value in database should not be null", trackingInfoFromDatabase);
        assertEquals("Unexpected value in database for tracking_info.jobTaskId",
                     "tenant-acme:job1", trackingInfoFromDatabase.getJobTaskId());
        assertNotNull("tracking_info.lastStatusCheckTime value in database should not be null",
                      trackingInfoFromDatabase.getLastStatusCheckTime());
        assertEquals("Unexpected value in database for tracking_info.statusCheckIntervalMillis",
                     TWO_MINUTES_IN_MILLIS, trackingInfoFromDatabase.getStatusCheckIntervalMillis());
        assertEquals("Unexpected value in database for tracking_info.statusCheckUrl",
                     MOCK_JOB_SERVICE_STATUS_CHECK_URL, trackingInfoFromDatabase.getStatusCheckUrl());
        assertEquals("Unexpected value in database for tracking_info.trackingPipe",
                     "dataprocessing-jobtracking-in", trackingInfoFromDatabase.getTrackingPipe());
        assertNull("tracking_info.trackTo value in database should be null", trackingInfoFromDatabase.getTrackTo());

        // source_info
        final TaskSourceInfo taskSourceInfoFromDatabase = OBJECT_MAPPER.readValue(stowedTaskRow.getSourceInfo(), TaskSourceInfo.class);
        assertNotNull("source_info value in database should not be null", taskSourceInfoFromDatabase);
        assertEquals("Unexpected value in database for source_info.name",
                     "agent1", taskSourceInfoFromDatabase.getName());
        assertEquals("Unexpected value in database for source_info.version",
                     "1.0", taskSourceInfoFromDatabase.getVersion());
    }

    @Test // Test stowing a task from a 'normal' worker, i.e. a Worker rather than a DocumentWorker.
    public void testStowWorkerTask() throws IOException, InterruptedException, Exception
    {
        // Given a task message
        final TrackingInfo trackingInfo = new TrackingInfo(
            "tenant-acme:job1",
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            MOCK_JOB_SERVICE_STATUS_CHECK_URL,
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
            "dataprocessing-elasticquery-in",
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
        assertEquals("Unexpected value in database for to", "dataprocessing-elasticquery-in", stowedTaskRow.getTo());
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
        final TrackingInfo trackingInfoFromDatabase = OBJECT_MAPPER.readValue(stowedTaskRow.getTrackingInfo(), TrackingInfo.class);
        assertNotNull("tracking_info value in database should not be null", trackingInfoFromDatabase);
        assertEquals("Unexpected value in database for tracking_info.jobTaskId",
                     "tenant-acme:job1", trackingInfoFromDatabase.getJobTaskId());
        assertNotNull("tracking_info.lastStatusCheckTime value in database should not be null",
                      trackingInfoFromDatabase.getLastStatusCheckTime());
        assertEquals("Unexpected value in database for tracking_info.statusCheckIntervalMillis",
                     TWO_MINUTES_IN_MILLIS, trackingInfoFromDatabase.getStatusCheckIntervalMillis());
        assertEquals("Unexpected value in database for tracking_info.statusCheckUrl",
                     MOCK_JOB_SERVICE_STATUS_CHECK_URL, trackingInfoFromDatabase.getStatusCheckUrl());
        assertEquals("Unexpected value in database for tracking_info.trackingPipe",
                     "dataprocessing-jobtracking-in", trackingInfoFromDatabase.getTrackingPipe());
        assertNull("tracking_info.trackTo value in database should be null", trackingInfoFromDatabase.getTrackTo());

        // source_info
        final TaskSourceInfo taskSourceInfoFromDatabase = OBJECT_MAPPER.readValue(stowedTaskRow.getSourceInfo(), TaskSourceInfo.class);
        assertNotNull("source_info value in database should not be null", taskSourceInfoFromDatabase);
        assertEquals("Unexpected value in database for source_info.name",
                     "agent1", taskSourceInfoFromDatabase.getName());
        assertEquals("Unexpected value in database for source_info.version",
                     "1.0", taskSourceInfoFromDatabase.getVersion());
    }

    @Test
    public void testTaskWithoutJobTaskIdIsNotStowed() throws IOException, InterruptedException, Exception
    {
        // Given a task message without a job task ID
        final TrackingInfo trackingInfo = new TrackingInfo(
            "",
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            MOCK_JOB_SERVICE_STATUS_CHECK_URL,
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
            "<dataprocessing-elasticquery-in",
            trackingInfo,
            null,
            null);

        // When the task message is sent to the worker
        queueServices.startListening();
        queueServices.sendTaskMessage(taskMessage);

        // Then the worker should have sent the message to it's output queue
        queueServices.waitForOutputQueueMessages(1, 30000);
        assertEquals("Expected 1 message to have been sent to the worker's output queue", 1,
                     queueServices.getOutputQueueMessages().size());

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
            MOCK_JOB_SERVICE_STATUS_CHECK_URL,
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
            "dataprocessing-elasticquery-in",
            trackingInfo,
            null,
            null);

        // When the task message is sent to the worker
        queueServices.startListening();
        queueServices.sendTaskMessage(taskMessage);

        // Then the worker should have sent the message to it's output queue
        queueServices.waitForOutputQueueMessages(1, 30000);
        assertEquals("Expected 1 message to have been sent to the worker's output queue", 1,
                     queueServices.getOutputQueueMessages().size());

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
            MOCK_JOB_SERVICE_STATUS_CHECK_URL,
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
            "dataprocessing-elasticquery-in",
            trackingInfo,
            null,
            null);

        // When the task message is sent to the worker
        queueServices.startListening();
        queueServices.sendTaskMessage(taskMessage);

        // Then the worker should have sent the message to it's output queue
        queueServices.waitForOutputQueueMessages(1, 30000);
        assertEquals("Expected 1 message to have been sent to the worker's output queue", 1,
                     queueServices.getOutputQueueMessages().size());

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

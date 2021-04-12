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

import com.google.gson.Gson;
import com.hpe.caf.api.worker.JobStatus;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import static com.microfocus.caf.worker.taskstowing.IntegrationTestSystemProperties.*;

final class MockJobServiceExpectationSetter
{
    private static final String EXPECTATION_URL = String.format("http://%s:%s/expectation", DOCKER_HOST_ADDRESS, MOCK_JOB_SERVICE_PORT);
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();
    private static final Gson GSON = new Gson();

    public static void addStatusCheckUrlExpectation(final String statusCheckUrlPath, final JobStatus jobStatus)
        throws IOException, InterruptedException
    {
        final MockJobServiceExpection mockJobServiceExpection =
            new MockJobServiceExpection("GET", statusCheckUrlPath, 200, jobStatus.name());
        final String statusCheckUrlExpectationJson = GSON.toJson(mockJobServiceExpection);
        final RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), statusCheckUrlExpectationJson);
        final Request request = new Request.Builder().url(EXPECTATION_URL).put(body).build();
        try (final Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 201) {
                throw new RuntimeException(
                    "Unexpected response code returned from mock job service PUT request. Expected 201 but got " + response.code());
            }
        }
    }

    private MockJobServiceExpectationSetter()
    {
    }
}

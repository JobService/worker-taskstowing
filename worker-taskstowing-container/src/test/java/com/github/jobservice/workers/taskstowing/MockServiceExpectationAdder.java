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

import com.google.gson.Gson;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import static com.github.jobservice.workers.taskstowing.IntegrationTestSystemProperties.*;

final class MockServiceExpectationAdder
{
    private static final String EXPECTATION_URL = String.format("http://%s:%s/expectation", DOCKER_HOST_ADDRESS, MOCK_SERVICE_PORT);
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();
    private static final Gson GSON = new Gson();

    public static void addExpectation(final MockServiceExpection mockServiceExpection)
        throws IOException, InterruptedException
    {
        final String expectationJson = GSON.toJson(mockServiceExpection);
        final RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), expectationJson);
        final Request request = new Request.Builder().url(EXPECTATION_URL).put(body).build();
        try (final Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            if (response.code() != 201) {
                throw new RuntimeException(
                    "Unexpected response code returned from mock service PUT expectation request. "
                    + "Expected 201 but got " + response.code());
            }
        }
    }

    private MockServiceExpectationAdder()
    {
    }
}

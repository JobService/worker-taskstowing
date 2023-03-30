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
package com.github.jobservice.workers.taskstowing;

final class MockServiceExpection
{
    public final HttpRequest httpRequest;
    public final HttpResponse httpResponse;

    public MockServiceExpection(final String method, final String path, final int statusCode, final String body)
    {
        this.httpRequest = new HttpRequest(method, path);
        this.httpResponse = new HttpResponse(statusCode, body);
    }

    private class HttpRequest
    {
        public final String method;
        public final String path;

        public HttpRequest(final String method, final String path)
        {
            this.method = method;
            this.path = path;
        }
    }

    private class HttpResponse
    {
        public final int statusCode;
        public final String body;

        public HttpResponse(final int statusCode, final String body)
        {
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}

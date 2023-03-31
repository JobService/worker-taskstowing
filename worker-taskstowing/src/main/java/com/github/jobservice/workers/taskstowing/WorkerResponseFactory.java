/*
 * Copyright 2021-2023 Open Text.
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

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskFailedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.WorkerResponse;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WorkerResponseFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerResponseFactory.class);

    private WorkerResponseFactory()
    {
    }

    static WorkerResponse createSuccessResultNoOutputToQueue(final String workerIdentifier, final int workerApiVersion)
    {
        return new WorkerResponse(null, TaskStatus.RESULT_SUCCESS, new byte[]{}, workerIdentifier, workerApiVersion, null);
    }

    static WorkerResponse createFailureResult(
        final String errorQueue,
        final String workerIdentifier,
        final int workerApiVersion,
        final Codec codec,
        final String failure)
    {
        try {
            byte[] data = codec.serialise(failure);
            return new WorkerResponse(errorQueue, TaskStatus.RESULT_FAILURE, data, workerIdentifier, workerApiVersion, null);
        } catch (final CodecException e) {
            throw new TaskFailedException("Failed to serialise result", e);
        }
    }

    static WorkerResponse createFailureResult(
        final String errorQueue,
        final String workerIdentifier,
        final int workerApiVersion,
        final Codec codec,
        final String failure,
        final Throwable cause)
    {
        final byte[] exceptionData = getExceptionData(new Exception(failure, cause), codec);
        return new WorkerResponse(errorQueue, TaskStatus.RESULT_FAILURE, exceptionData, workerIdentifier, workerApiVersion, null);

    }

    static WorkerResponse createExceptionResult(
        final String errorQueue,
        final String workerIdentifier,
        final int workerApiVersion,
        final Codec codec,
        final Throwable cause)
    {
        return new WorkerResponse(
            errorQueue,
            TaskStatus.RESULT_EXCEPTION,
            WorkerResponseFactory.getExceptionData(cause, codec),
            workerIdentifier,
            workerApiVersion,
            null);
    }

    static byte[] getExceptionData(final Throwable t, final Codec codec)
    {
        try {
            final String exceptionDetail = buildExceptionStackTrace(t);
            return codec.serialise(exceptionDetail);
        } catch (final CodecException e) {
            LOGGER.warn("Failed to serialise exception, continuing", e);
            return new byte[]{};
        }
    }

    private static String buildExceptionStackTrace(final Throwable e)
    {
        // Build up exception detail from stack trace
        final StringBuilder exceptionStackTrace = new StringBuilder(e.getClass() + " " + e.getMessage());
        // Check if there is a stack trace on the exception before building it up into a string
        if (Objects.nonNull(e.getStackTrace())) {
            exceptionStackTrace.append(stackTraceToString(e.getStackTrace()));
        }
        // If a cause exists add it to the exception detail
        if (Objects.nonNull(e.getCause())) {
            exceptionStackTrace.
                append(". Cause: ").append(e.getCause().getClass().toString()).append(" ").append(e.getCause().getMessage());
            // Check if the cause has a stack trace before building it up into a string
            if (Objects.nonNull(e.getCause().getStackTrace())) {
                exceptionStackTrace.append(stackTraceToString(e.getCause().getStackTrace()));
            }
        }
        return exceptionStackTrace.toString();
    }

    private static String stackTraceToString(final StackTraceElement[] stackTraceElements)
    {
        final StringBuilder stackTraceStr = new StringBuilder();
        // From each stack trace element, build up the stack trace
        for (final StackTraceElement stackTraceElement : stackTraceElements) {
            stackTraceStr.append(" ").append(stackTraceElement.toString());
        }
        return stackTraceStr.toString();
    }
}

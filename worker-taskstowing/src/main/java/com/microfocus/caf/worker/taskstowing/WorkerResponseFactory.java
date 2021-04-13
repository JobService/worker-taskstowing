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

    public static WorkerResponse createSuccessResultNoOutputToQueue(final String workerIdentifier, final int workerApiVersion)
    {
        return new WorkerResponse(null, TaskStatus.RESULT_SUCCESS, new byte[]{}, workerIdentifier, workerApiVersion, null);
    }

    public static WorkerResponse createFailureResult(
        final String outputQueue,
        final String workerIdentifier,
        final int workerApiVersion,
        final Codec codec,
        final String failure)
    {
        try {
            byte[] data = codec.serialise(failure);
            return new WorkerResponse(outputQueue, TaskStatus.RESULT_FAILURE, data, workerIdentifier, workerApiVersion, null);
        } catch (final CodecException e) {
            throw new TaskFailedException("Failed to serialise result", e);
        }
    }

    public static WorkerResponse createFailureResult(
        final String outputQueue,
        final String workerIdentifier,
        final int workerApiVersion,
        final Codec codec,
        final String failure,
        final Throwable cause)
    {
        final byte[] exceptionData = getExceptionData(new Exception(failure, cause), codec);
        return new WorkerResponse(outputQueue, TaskStatus.RESULT_FAILURE, exceptionData, workerIdentifier, workerApiVersion, null);

    }

    public static byte[] getExceptionData(final Throwable t, final Codec codec)
    {
        try {
            String exceptionDetail = buildExceptionStackTrace(t);
            return codec.serialise(exceptionDetail);
        } catch (CodecException e) {
            LOGGER.warn("Failed to serialise exception, continuing", e);
            return new byte[]{};
        }
    }

    private static String buildExceptionStackTrace(final Throwable e)
    {
        // Build up exception detail from stack trace
        StringBuilder exceptionStackTrace = new StringBuilder(e.getClass() + " " + e.getMessage());
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
        StringBuilder stackTraceStr = new StringBuilder();
        // From each stack trace element, build up the stack trace
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            stackTraceStr.append(" ").append(stackTraceElement.toString());
        }
        return stackTraceStr.toString();
    }
}

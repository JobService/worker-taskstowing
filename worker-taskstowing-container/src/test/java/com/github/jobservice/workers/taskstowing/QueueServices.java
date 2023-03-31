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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.hpe.caf.api.worker.TaskMessage;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import static com.github.jobservice.workers.taskstowing.IntegrationTestSystemProperties.*;

/**
 * This class is responsible sending task data to the target queue.
 */
public final class QueueServices
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueServices.class);

    private static final Map<String, List<String>> QUEUE_MESSAGES = new ConcurrentHashMap<>();

    private final Connection connection;
    private final Channel publisherChannel;
    private final Channel errorChannel;
    private final Channel outputChannel;
    private final Channel forwardChannel;
    private final String targetQueueName;
    private final String errorQueueName;
    private final String outputQueueName;
    private final String forwardQueueName;

    private final HttpHost rabbitHost;
    private final HttpClientContext httpContext;

    public QueueServices() throws IOException, TimeoutException
    {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(DOCKER_HOST_ADDRESS);
        factory.setPort(Integer.parseInt(RABBITMQ_NODE_PORT));
        factory.setUsername("guest");
        factory.setPassword("guest");
        LOGGER.info("Getting Rabbit MQ connection...");
        this.connection = factory.newConnection();
        LOGGER.info("Creating Rabbit MQ channels...");
        this.publisherChannel = connection.createChannel();
        this.publisherChannel.confirmSelect();
        this.errorChannel = connection.createChannel();
        this.outputChannel = connection.createChannel();
        this.forwardChannel = connection.createChannel();

        this.targetQueueName = "worker-taskstowing-in";
        this.outputQueueName = "worker-taskstowing-out";
        this.errorQueueName = "worker-taskstowing-err";
        this.forwardQueueName = "dataprocessing-elasticquery-in";
        
        QUEUE_MESSAGES.put(outputQueueName, new ArrayList<>());
        QUEUE_MESSAGES.put(errorQueueName, new ArrayList<>());
        QUEUE_MESSAGES.put(forwardQueueName, new ArrayList<>());

        LOGGER.info("Declare target worker queue...");
        publisherChannel.queueDeclare(targetQueueName, true, false, false, null);
        LOGGER.info("Declare worker error queue...");
        errorChannel.queueDeclare(errorQueueName, true, false, false, null);
        LOGGER.info("Declare worker output queue...");
        outputChannel.queueDeclare(outputQueueName, true, false, false, null);
        LOGGER.info("Declare worker forward queue...");
        forwardChannel.queueDeclare(forwardQueueName, true, false, false, null);

        rabbitHost = new HttpHost(DOCKER_HOST_ADDRESS, Integer.parseInt(RABBITMQ_CTRL_PORT),
                                  "http");
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                                     new UsernamePasswordCredentials("guest", "guest"));

        AuthCache authCache = new BasicAuthCache();
        authCache.put(rabbitHost, new BasicScheme());
        // Add AuthCache to the execution httpContext
        httpContext = HttpClientContext.create();
        httpContext.setCredentialsProvider(credsProvider);
        httpContext.setAuthCache(authCache);
    }

    public void sendTaskMessage(final TaskMessage taskMessage) throws IOException, InterruptedException
    {
        // Serialize the task message.
        // Wrap any CodecException as a RuntimeException as it shouldn't happen
        final byte[] taskMessageBytes;
        try {
            taskMessageBytes = serializeObject(taskMessage);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Send the message.
        publisherChannel.basicPublish("", targetQueueName, MessageProperties.TEXT_PLAIN, taskMessageBytes);
        publisherChannel.waitForConfirms();
        LOGGER.info("*************** Send a message to the " + targetQueueName + " Rabbit MQ queue: "
            + new String(taskMessageBytes, "UTF-8"));
    }

    public void waitForInputQueue(int timeoutMs)
    {
        int sleepTime = 0;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            while (sleepTime < timeoutMs) {
                sleepTime += 1000;
                Thread.sleep(1000);
                try (CloseableHttpResponse resp = client.execute(rabbitHost,
                                                                 new HttpGet("/api/queues/%2F/" + targetQueueName), httpContext)) {
                    String body = EntityUtils.toString(resp.getEntity());
                    LOGGER.info("Queue stats: {}", body);
                    JsonNode jsonNode = new ObjectMapper().readTree(body);
                    int msgCount = jsonNode.get("messages_ready").asInt(-1)
                        + jsonNode.get("messages_unacknowledged").asInt(-1);
                    if (msgCount == 0) {
                        return;
                    }
                }
            }
        } catch (final Exception e) {
            if (e instanceof NullPointerException) {
                e.printStackTrace();
            }
            Assert.fail("Error waiting on input queue to clear: " + e.getMessage());
        }
    }

    public void waitForOutputQueueMessages(int msgCount, int timeoutMs) throws InterruptedException
    {
        waitForMessages(msgCount, timeoutMs, outputQueueName);
    }

    public void waitForErrorQueueMessages(int msgCount, int timeoutMs) throws InterruptedException
    {
        waitForMessages(msgCount, timeoutMs, errorQueueName);
    }

    public void waitForForwardQueueMessages(int msgCount, int timeoutMs) throws InterruptedException
    {
        waitForMessages(msgCount, timeoutMs, forwardQueueName);
    }

    public List<String> getOutputQueueMessages()
    {
        return QUEUE_MESSAGES.get(outputQueueName);
    }

    public List<String> getErrorQueueMessages()
    {
        return QUEUE_MESSAGES.get(errorQueueName);
    }

    public List<String> getForwardQueueMessages()
    {
        return QUEUE_MESSAGES.get(forwardQueueName);
    }

    public int getMessageCount(final String queueName)
    {
        return QUEUE_MESSAGES.get(queueName).size();
    }

    private void waitForMessages(final int msgCount, final int timeoutMs, final String queueName) throws InterruptedException
    {
        int sleepTime = 0;
        do {
            if (getMessageCount(queueName) >= msgCount) {
                return;
            }
            sleepTime += 1000;
            Thread.sleep(1000);
        } while (sleepTime < timeoutMs);
    }

    private void getMessage(final Channel channel, final String queue) throws IOException
    {
        final String watchedQueue = "[" + queue + " Queue]";

        LOGGER.info("************** Watching queue : " + watchedQueue);

        final Consumer consumer = new DefaultConsumer(channel)
        {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException
            {
                final String message = new String(body, "UTF-8");
                LOGGER.info("-------------------------" + watchedQueue + " Received '" + message + "' {}", envelope.getDeliveryTag());
                if (queue.equalsIgnoreCase(errorQueueName)) {
                    QUEUE_MESSAGES.get(errorQueueName).add(message);
                } else if (queue.equalsIgnoreCase(outputQueueName)) {
                    QUEUE_MESSAGES.get(outputQueueName).add(message);
                } else if (queue.equalsIgnoreCase(forwardQueueName)) {
                    QUEUE_MESSAGES.get(forwardQueueName).add(message);
                }
                channel.basicAck(envelope.getDeliveryTag(), true);
            }
        };
        final boolean autoAck = false; // acknowledgment is covered below
        channel.basicConsume(queue, autoAck, consumer);
    }

    public void close() throws Exception
    {
        try {
            // Close channel.
            if (publisherChannel != null) {
                publisherChannel.close();
            }

            if (errorChannel != null) {
                errorChannel.close();
            }

            if (outputChannel != null) {
                outputChannel.close();
            }

            if (forwardChannel != null) {
                forwardChannel.close();
            }

            // Close connection.
            if (connection != null) {
                connection.close();
            }

        } catch (final IOException | TimeoutException e) {
            throw new Exception("Failed to close the queuing connection.");
        }
    }

    private byte[] serializeObject(final Object object) throws JsonProcessingException
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new GuavaModule());

        return mapper.writeValueAsBytes(object);
    }

    public void startListening()
    {
        try {
            getMessage(outputChannel, outputQueueName);
            getMessage(errorChannel, errorQueueName);
            getMessage(forwardChannel, forwardQueueName);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}

package io.tolgee.jobs

import io.tolgee.jobs.testutil.PostgresExtension
import io.tolgee.jobs.testutil.RedisExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration

@SpringBootTest
@Testcontainers
@ExtendWith(RedisExtension::class)
@ExtendWith(PostgresExtension::class)
class RedisContainerTest {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            RedisExtension.registerPgProps(registry)
            PostgresExtension.registerPgProps(registry)
        }
    }

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @Test
    fun testRedisPubSub() {
        // Create a topic
        val topic = redissonClient.getTopic("test-topic")

        // Create a latch to wait for message
        val latch = CountDownLatch(1)
        val receivedMessages = mutableListOf<String>()

        // Subscribe to the topic
        val listenerId = topic.addListener(String::class.java) { channel, message ->
            println("[DEBUG_LOG] Received message: $message on channel: $channel")
            receivedMessages.add(message)
            latch.countDown()
        }

        try {
            // Publish a message
            val testMessage = "Hello Redis PubSub!"
            val receiverCount = topic.publish(testMessage)

            println("[DEBUG_LOG] Message published to $receiverCount receivers")

            // Wait for the message to be received
            val received = latch.await(5, TimeUnit.SECONDS)

            // Assertions
            assertTrue(received, "Message should be received within timeout")
            assertEquals(1, receivedMessages.size, "Should receive exactly one message")
            assertEquals(testMessage, receivedMessages[0], "Received message should match sent message")
        } finally {
            // Clean up
            topic.removeListener(listenerId)
        }
    }
}

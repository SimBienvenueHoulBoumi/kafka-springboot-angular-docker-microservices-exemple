package com.simdev.api_users.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
@SpringBootTest
@EmbeddedKafka(topics = {"user-events"}, partitions = 1)
@DisplayName("User Kafka Tests")
class UserKafkaTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> configs = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumer = new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        consumer.subscribe(Collections.singletonList("user-events"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should send message to Kafka topic")
    void testSendMessageToKafka() {
        // Given
        String key = "user.created";
        String message = "User created: test@example.com (1)";

        // When
        kafkaTemplate.send("user-events", key, message);

        // Then
        ConsumerRecord<String, String> received = KafkaTestUtils.getSingleRecord(consumer, "user-events");
        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo(key);
        assertThat(received.value()).isEqualTo(message);
    }
}


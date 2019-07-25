package io.openliberty.guides.job;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class JobConsumer implements Runnable {

    private Consumer<String, String> consumer;
    private JobManager manager;

    private final String OFFSET_RESET_CONFIG = "earliest";

    public JobConsumer(JobManager manager, String kafkaServer, String groupIdPrefix) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, String.format("%s-%s", groupIdPrefix, UUID.randomUUID().toString()));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET_CONFIG);

        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Arrays.asList("job-result-topic"));

        this.manager = manager;
    }

    private List<String> consumeMessages() {
        List<String> result = new ArrayList<String>();
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));

        for (ConsumerRecord<String, String> record : records) {
            result.add(record.value());
        }

        consumer.commitAsync();
        return result;
    }

    @Override
    public void run() {
        Jsonb jsonb = JsonbBuilder.create();

        while(true) {
            List<JobResultModel> results = consumeMessages()
                .stream()
                .map(m -> jsonb.fromJson(m, JobResultModel.class))
                .collect(Collectors.toList());

            for (JobResultModel r : results) {
                manager.addResult(r.getJobId(), r.getResult());
            }
        }
    }

}

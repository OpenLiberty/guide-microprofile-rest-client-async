package io.openliberty.guides.system;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
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

public class SystemRunnable implements Runnable {

    private SystemProducer producer;
    private Consumer<String, String> consumer;

    private final String CONSUMER_OFFSET_RESET = "earliest";

    public SystemRunnable(String kafkaServer, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, CONSUMER_OFFSET_RESET);

        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Arrays.asList("job-topic"));

        producer = new SystemProducer();
    }

    @Override
    public void run() {
        Random rand = new Random();
        Jsonb jsonb = JsonbBuilder.create();

        producer.sendMessage("system-topic", jsonb.toJson(getProperties(false)));

        while (true) {
            List<JobModel> jobs = consumeMessages().stream().map(m -> jsonb.fromJson(m, JobModel.class))
                    .collect(Collectors.toList());

            for (JobModel job : jobs) {
                producer.sendMessage("system-topic", jsonb.toJson(getProperties(true)));

                int sleepTimeSeconds = rand.nextInt(5) + 5; // 5 to 10
                int result = sleepTimeSeconds;

                try {
                    Thread.sleep(sleepTimeSeconds * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    producer.sendMessage("job-result-topic", jsonb.toJson(new JobResultModel(job.getJobId(), result)));
                }

                producer.sendMessage("system-topic", jsonb.toJson(getProperties(false)));
            }
        }
    }

    private Properties getProperties(boolean isBusy) {
        Properties props = (Properties) System.getProperties().clone();
        props.setProperty("hostname", System.getenv("HOSTNAME"));
        props.setProperty("system.busy", Boolean.toString(isBusy));
        return props;
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

}

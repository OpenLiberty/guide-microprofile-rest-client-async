package io.openliberty.guides.job;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JobStartup {
    private JobConsumer consumer;

    @Resource
    private ManagedExecutorService executorService;

    @Inject
    private JobManager manager;

    @Inject
    @ConfigProperty(name = "KAFKA_SERVER")
    private String kafkaServer;

    @Inject
    @ConfigProperty(name = "GROUP_ID_PREFIX")
    private String groupIdPrefix;

    private void init(@Observes @Initialized(ApplicationScoped.class) Object x) {
        this.consumer = new JobConsumer(manager, kafkaServer, groupIdPrefix);
        executorService.execute(consumer);
    }
}

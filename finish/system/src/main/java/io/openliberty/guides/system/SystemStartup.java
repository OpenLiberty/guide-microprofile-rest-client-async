package io.openliberty.guides.system;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SystemStartup {
    private SystemRunnable runnable;

    @Resource
    private ManagedExecutorService executorService;

    @Inject
    @ConfigProperty(name = "KAFKA_SERVER")
    private String kafkaServer;

    @Inject
    @ConfigProperty(name = "GROUP_ID")
    private String groupId;

    private void init(@Observes @Initialized(ApplicationScoped.class) Object x) {
        runnable = new SystemRunnable(kafkaServer, groupId);
        executorService.execute(runnable);
    }
}

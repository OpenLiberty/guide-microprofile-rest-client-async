package io.openliberty.guides.job;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JobManager {
    private Map<String, Integer> jobResults = Collections.synchronizedMap(new HashMap<String, Integer>());

    public void addResult(String jobId, Integer result) {
        jobResults.put(jobId, result);
    }

    public Optional<Integer> getResult(String jobId) {
        Integer result = jobResults.get(jobId);
        return Optional.ofNullable(result);
    }

    public Map<String, Integer> getResults() {
        return new HashMap<>(jobResults);
    }
}

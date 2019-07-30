package io.openliberty.guides.job;

import java.util.Collections;
import java.util.List;

public class JobsModel {
    private List<JobResultModel> results;

    public JobsModel() {
        this.results = Collections.emptyList();
    }

    public JobsModel(List<JobResultModel> results) {
        this.results = results;
    }

    public List<JobResultModel> getResults() {
        return results;
    }

    public void setResults(List<JobResultModel> results) {
        this.results = results;
    }
}

package io.openliberty.guides.models;

import java.util.List;

public class JobsModel {
    private List<JobResultModel> results;

    public List<JobResultModel> getResults() {
        return results;
    }

    public void setResults(List<JobResultModel> results) {
        this.results = results;
    }
    
}
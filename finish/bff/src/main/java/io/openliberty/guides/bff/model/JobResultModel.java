package io.openliberty.guides.bff.model;

public class JobResultModel {
    private String jobId;
    private Integer result;

    public Integer getResult() {
        return result;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

}
package io.openliberty.guides.system;

public class JobResultModel {
    private String jobId;
    private Integer result;

    public JobResultModel() {
        this.jobId = null;
        this.result = null;
    }

    public JobResultModel(String jobId, Integer result) {
        this.jobId = jobId;
        this.result = result;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public Integer getResult() {
        return result;
    }

    public String getJobId() {
        return jobId;
    }

}

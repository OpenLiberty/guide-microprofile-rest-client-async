package io.openliberty.guides.job;

public class JobResultModel {
    private String jobId;
    private Integer result;

    public JobResultModel() {
        this.setJobId(null);
        this.setResult(null);
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobResultModel(String jobId, Integer result) {
        this.setJobId(jobId);
        this.setResult(result);
    }
}

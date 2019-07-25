package io.openliberty.guides.job;

public class JobModel {
    private String jobId;

    public JobModel() {
        this.jobId = null;
    }

    public JobModel(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}

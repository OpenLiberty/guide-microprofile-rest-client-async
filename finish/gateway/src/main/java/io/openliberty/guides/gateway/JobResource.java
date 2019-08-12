package io.openliberty.guides.gateway;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.openliberty.guides.gateway.client.JobClient;
import io.openliberty.guides.models.JobListModel;
import io.openliberty.guides.models.JobModel;
import io.openliberty.guides.models.JobResultModel;

@Path("/jobs")
public class JobResource {

    @Inject
    @RestClient
    private JobClient jobClient;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JobListModel> getJobs() {
        return jobClient
            .getJobs()
            // tag::thenApplyAsync[]
            .thenApplyAsync((jobs) -> {
                return new JobListModel(jobs.getResults());
            })
            // end::thenApplyAsync[]
            // tag::exceptionally[]
            .exceptionally((ex) -> {
                // Respond with empty list on error
                return new JobListModel();
            });
            // end::exceptionally[]
    }

    @GET
    @Path("{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JobResultModel> getJob(@PathParam("jobId") String jobId) {
        return jobClient.getJob(jobId);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JobModel> createJob() {
        return jobClient.createJob();
    }
}

package io.openliberty.guides.bff.client;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.openliberty.guides.bff.model.JobModel;
import io.openliberty.guides.bff.model.JobResultModel;
import io.openliberty.guides.bff.model.JobsModel;

@RegisterRestClient(baseUri = "http://job-service:9080")
@Path("/jobs")
public interface JobClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JobsModel> getJobs();

    @GET
    @Path("{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JobResultModel> getJob(@PathParam("jobId") String jobId);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JobModel> createJob();

}

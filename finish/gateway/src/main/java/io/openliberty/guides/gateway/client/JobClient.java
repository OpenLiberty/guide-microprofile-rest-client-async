package io.openliberty.guides.gateway.client;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.openliberty.guides.models.JobModel;
import io.openliberty.guides.models.JobResultModel;
import io.openliberty.guides.models.JobsModel;

@RegisterRestClient(baseUri = "http://job-service:9080")
@Path("/jobs")
public interface JobClient {

    // tag::getJobs[]
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JobsModel> getJobs();
    // end::getJobs[]

    // tag::getJob[]
    @GET
    @Path("{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JobResultModel> getJob(@PathParam("jobId") String jobId);
    // end::getJob[]

    // tag::createJob[]
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JobModel> createJob();
    // end::createJob[]

}

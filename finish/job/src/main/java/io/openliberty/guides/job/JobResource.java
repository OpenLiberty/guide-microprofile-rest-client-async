// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// CDI
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
// JAX-RS
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.openliberty.guides.models.JobModel;
import io.openliberty.guides.models.JobResultModel;
import io.openliberty.guides.models.JobsModel;

@RequestScoped
@Path("/jobs")
public class JobResource {

  @Inject
  private JobProducer producer;

  @Inject
  private JobManager manager;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public JobModel createJob() {
    String jobId = UUID.randomUUID().toString();
    JobModel job = new JobModel();
    job.setJobId(jobId);

    Jsonb jsonb = JsonbBuilder.create();
    producer.sendMessage("job-topic", jsonb.toJson(job));

    return job;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JobsModel getJobResults() {
    List<JobResultModel> results = manager.getResults()
      .entrySet()
      .stream()
      .map(es -> creatResultModel(es.getKey(), es.getValue()))
      .collect(Collectors.toList());

    JobsModel responseModel = new JobsModel();
    responseModel.setResults(results);
    return responseModel;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{jobId}")
  public Response getJobResult(@PathParam("jobId") String jobId) {
    Optional<JobResultModel> model = manager
      .getResult(jobId)
      .map(r -> creatResultModel(jobId, r));

    if (model.isPresent()) {
      return Response.ok(model.get()).build();
    }

    return Response.status(Status.NOT_FOUND).build();
  }

  private JobResultModel creatResultModel(String jobId, int result) {
    JobResultModel resultModel = new JobResultModel();
    resultModel.setJobId(jobId);
    resultModel.setResult(result);
    return resultModel;
  }

}

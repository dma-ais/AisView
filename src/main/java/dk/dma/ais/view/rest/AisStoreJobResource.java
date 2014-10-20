/* Copyright (c) 2011 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.ais.view.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.store.job.JobManager;
import dk.dma.ais.store.job.JobManager.Job;
import dk.dma.commons.util.JSONObject;
import dk.dma.commons.web.rest.AbstractResource;
/**
 * 
 * @author Jens Tuxen
 *
 */
@Path("/store/job")
public class AisStoreJobResource extends AbstractResource {
    
    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping(@Context UriInfo info) {
        return "pong";
    }
    
    @GET
    @Path("/")
    @Produces("application/json")
    public JSONObject root(@Context UriInfo info) {
        return all(info);
    }
    
    @GET
    @Path("/all")
    @Produces("application/json")
    public JSONObject all(@Context UriInfo info) {
        return get(JobManager.class).toJSON();
    }
    
    /**
     * Used to query job status for long running AisStore access jobs.
     */
    @GET
    @Produces("application/json")
    @Path("/status/{jobId : \\w+}")
    public JSONObject queryStatus(@Context UriInfo info,
            @PathParam("jobId") String jobId) {
        if (jobId != null) {
            Job j = get(JobManager.class).getResult(jobId);
            if (j != null) {
                return j.toJSON();
            }
        }
        return new JSONObject();
    }

}

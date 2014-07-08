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

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.tracker.TargetTracker;
import dk.dma.commons.web.rest.AbstractResource;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class RuntimeResource extends AbstractResource {

    @GET
    @Produces("text/plain")
    @Path("/targetCount")
    public int targetCount(@Context ServletConfig config, @Context UriInfo info) {
        QueryParameterHelper qh = new QueryParameterHelper(info);
        return get(TargetTracker.class).countNumberOfTargets(qh.getSourcePredicate(), qh.getTargetPredicate());
    }

    @GET
    @Produces("text/plain")
    @Path("/reportCount")
    public int reportCount(@Context UriInfo info) {
        return get(TargetTracker.class).countNumberOfReports(new QueryParameterHelper(info).getSourceAndTargetPredicate());
    }
}

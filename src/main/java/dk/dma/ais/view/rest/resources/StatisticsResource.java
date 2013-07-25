/*
 * Copyright (c) 2008 Kasper Nielsen.
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
package dk.dma.ais.view.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class StatisticsResource extends AbstractViewerResource {

    @GET
    @Produces("text/plain")
    @Path("/targetCount")
    public int targetCount(final @Context UriInfo info) {
        return getTracker().countNumberOfTargets(getSourceFilter(info));
    }

    @GET
    @Produces("text/plain")
    @Path("/reportCount")
    public int reportCount(final @Context UriInfo info) {
        return getTracker().countNumberOfReports(getSourceFilter(info));
    }
}

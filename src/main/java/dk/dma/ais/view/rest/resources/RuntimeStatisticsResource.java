/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.ais.view.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.view.rest.resources.util.QueryParser;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class RuntimeStatisticsResource extends AbstractViewerResource {

    @GET
    @Produces("text/plain")
    @Path("/targetCount")
    public int targetCount(@Context UriInfo info) {
        return getTracker().countNumberOfTargets(new QueryParser(info).getSourcePredicate());
    }

    @GET
    @Produces("text/plain")
    @Path("/reportCount")
    public int reportCount(@Context UriInfo info) {
        return getTracker().countNumberOfReports(new QueryParser(info).getSourcePredicate());
    }
}

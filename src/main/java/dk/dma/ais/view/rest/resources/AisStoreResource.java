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
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.store.AisStoreQueryBuilder;
import dk.dma.ais.view.rest.resources.util.QueryParser;
import dk.dma.commons.web.rest.StreamingUtil;
import dk.dma.commons.web.rest.UriQueryUtil;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class AisStoreResource extends AbstractViewerResource {

    @GET
    @Produces("text/plain")
    @Path("/store")
    public StreamingOutput foo(@Context final UriInfo info) {
        QueryParser p = new QueryParser(info);

        // Create builder
        AisStoreQueryBuilder b;
        if (p.getMMSIs().length > 0) {
            b = AisStoreQueryBuilder.forMmsi(p.getMMSIs());
        } else if (p.getArea() != null) {
            b = AisStoreQueryBuilder.forArea(p.getArea());
        } else {
            b = AisStoreQueryBuilder.forTime();
        }
        b.setBatchLimit(UriQueryUtil.getOneOrZeroIntParametersOrFail(info, "fetchSize", 3000));
        b.setInterval(p.getInterval());

        // create query and apply filters
        Iterable<AisPacket> query = getStore().execute(b);
        query = p.applySourceFilter(query);
        query = p.applyLimitFilter(query); // must be the last if other filters reject packets

        return StreamingUtil.createStreamingOutput(query, p.getOutputSink());
    }
}

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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.packet.AisPacketOutputSinks;
import dk.dma.ais.store.AisStoreQueryBuilder;
import dk.dma.ais.view.rest.resources.util.QueryParser;
import dk.dma.commons.util.Iterables;
import dk.dma.commons.web.rest.StreamingUtil;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class TargetResource extends AbstractViewerResource {

    @GET
    @Path("/track/{mmsi : \\d+}")
    @Produces("application/json")
    public StreamingOutput pasttrack(@PathParam("mmsi") int mmsi, @Context UriInfo info) {
        QueryParser p = new QueryParser(info);

        AisStoreQueryBuilder b = AisStoreQueryBuilder.forMmsi(mmsi);
        b.setInterval(p.getInterval());

        Iterable<AisPacket> query = getStore().execute(b);
        query = Iterables.filter(query, AisPacketFilters.filterOnMessageType(IVesselPositionMessage.class));
        query = p.applySourceFilter(query);
        query = p.applyPositionSampler(query);
        query = p.applyLimitFilter(query);
        return StreamingUtil.createStreamingOutput(query, AisPacketOutputSinks.PAST_TRACK_JSON);
    }
}

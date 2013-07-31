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

import static dk.dma.ais.view.rest.resources.util.QueryParameterParser.findArea;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.joda.time.Interval;

import com.google.common.primitives.Ints;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketOutputStreamSinks;
import dk.dma.ais.view.rest.resources.util.QueryParameterParser;
import dk.dma.commons.web.rest.StreamingUtil;
import dk.dma.commons.web.rest.UriQueryUtil;
import dk.dma.enav.model.geometry.Area;

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
        Interval interval = QueryParameterParser.findInterval(info);

        Set<Integer> mmsi = new HashSet<>(UriQueryUtil.getParametersAsInt(info, "mmsi"));

        Area area = findArea(info);

        final Iterable<AisPacket> query;
        if (mmsi.size() > 0) {
            query = getStore().findForMmsi(interval.getStartMillis(), interval.getEndMillis(), Ints.toArray(mmsi));
        } else if (area != null) {
            query = getStore().findForArea(area, interval.getStartMillis(), interval.getEndMillis());
        } else {
            query = getStore().findForTime(interval.getStartMillis(), interval.getEndMillis());
        }

        return StreamingUtil.createStreamingOutput(query, AisPacketOutputStreamSinks.OUTPUT_TO_TEXT);
    }
}

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

import static dk.dma.ais.view.rest.resources.util.QueryParameterParser.findInterval;
import static dk.dma.ais.view.rest.resources.util.QueryParameterParser.getSourceFilter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.joda.time.Interval;

import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.packet.AisPacketOutputStreamSinks;
import dk.dma.ais.view.rest.resources.util.QueryParameterParser;
import dk.dma.commons.util.Iterables;
import dk.dma.commons.web.rest.StreamingUtil;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.util.function.Predicate;

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
        Predicate<AisPacket> f = getSourceFilter(info);
        f = f.and(AisPacketFilters.filterOnMessageType(IVesselPositionMessage.class));

        Integer minDistance = QueryParameterParser.findMinimumDistanceMeters(info);
        Long minDuration = QueryParameterParser.findMinimumDurationMS(info);
        if (minDistance != null || minDuration != null) {
            f = f.and(new Sampler(minDistance, minDuration));
        }

        Interval interval = findInterval(info);
        Iterable<AisPacket> q = getStore().findForMmsi(interval.getStartMillis(), interval.getEndMillis(), mmsi);
        q = Iterables.filter(q, f);

        return StreamingUtil.createStreamingOutput(q, AisPacketOutputStreamSinks.PAST_TRACK_JSON);
    }

    static class Sampler extends Predicate<AisPacket> {
        Position lastPosition;
        Long lastTimestamp;
        final Long sampleDurationMS;
        final Integer samplePositionMeters;

        Sampler(Integer minDistance, Long minDuration) {
            this.samplePositionMeters = minDistance;
            this.sampleDurationMS = minDuration;
        }

        /** {@inheritDoc} */
        @Override
        public boolean test(AisPacket p) {
            Position pos = p.tryGetAisMessage().getValidPosition();
            try {
                if (samplePositionMeters != null
                        && (lastPosition == null || lastPosition.rhumbLineDistanceTo(pos) >= samplePositionMeters)) {
                    return true;
                }
                return sampleDurationMS != null
                        && (lastTimestamp == null || p.getBestTimestamp() - lastTimestamp >= sampleDurationMS);
            } finally {
                lastPosition = pos;
                lastTimestamp = p.getBestTimestamp();
            }
        }
    }
}

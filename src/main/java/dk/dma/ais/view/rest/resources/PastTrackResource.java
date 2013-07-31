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

import static dk.dma.ais.view.rest.resources.util.ParameterExtractor.findInterval;
import static dk.dma.ais.view.rest.resources.util.ParameterExtractor.getSourceFilter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.joda.time.Interval;
import org.joda.time.Period;

import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.packet.AisPacketOutputStreamSinks;
import dk.dma.commons.util.Iterables;
import dk.dma.commons.web.rest.StreamingUtil;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.util.function.Predicate;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class PastTrackResource extends AbstractViewerResource {

    @SuppressWarnings("rawtypes")
    @GET
    @Path("/track2/{mmsi : \\d+}")
    @Produces("application/json")
    public StreamingOutput json2(@PathParam("mmsi") int mmsi, @Context UriInfo info) {
        Interval interval = findInterval(info);
        Iterable<AisPacket> q = getStore().findForMmsi(interval.getStartMillis(), interval.getEndMillis(), mmsi);

        Predicate<AisPacket> f = getSourceFilter(info);
        f = f.and(AisPacketFilters.filterOnMessageType(IVesselPositionMessage.class));

        f = f.and(new Sampler(info));

        if (f != (Predicate) Predicate.TRUE) {
            q = Iterables.filter(q, f);
        }

        return StreamingUtil.createStreamingOutput(q, AisPacketOutputStreamSinks.PAST_TRACK_JSON);
    }

    static class Sampler extends Predicate<AisPacket> {
        Position lastPosition;
        long lastTimestamp = Long.MIN_VALUE;
        final Long sampleDurationMS;
        final Integer samplePositionMeters;

        Sampler(UriInfo info) {
            String sp = info.getQueryParameters().getFirst("minDistance");
            String dur = info.getQueryParameters().getFirst("minDuration");
            samplePositionMeters = sp == null ? null : Integer.parseInt(sp);
            sampleDurationMS = dur == null ? null : Period.parse(dur).toStandardSeconds().getSeconds() * 1000L;
        }

        /** {@inheritDoc} */
        @Override
        public boolean test(AisPacket p) {
            Position pos = p.tryGetAisMessage().getValidPosition();
            try {
                if (sampleDurationMS == null && samplePositionMeters == null) {
                    return true;
                }
                if (samplePositionMeters != null
                        && (lastPosition == null || lastPosition.rhumbLineDistanceTo(pos) >= samplePositionMeters)) {
                    return true;
                }
                return sampleDurationMS != null && p.getBestTimestamp() - lastTimestamp >= sampleDurationMS;
            } finally {
                lastPosition = pos;
                lastTimestamp = p.getBestTimestamp();
            }
        }
    }

}

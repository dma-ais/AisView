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

import static dk.dma.ais.view.rest.resources.util.QueryParameterParser.getOutputSink;
import static dk.dma.ais.view.rest.resources.util.QueryParameterParser.getSourceFilter;
import static dk.dma.commons.web.rest.UriQueryUtil.getOneOrZeroParametersOrFail;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketStream;
import dk.dma.ais.packet.AisPacketStream.Subscription;
import dk.dma.enav.util.function.Predicate;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class StreamResource extends AbstractViewerResource {

    /**
     * Creates a stream of all incoming data from all sources.
     * 
     * @param info
     *            the URL request info
     * @return a packet stream
     */
    AisPacketStream createStream(UriInfo info) {
        AisPacketStream s = newLiveStream();

        Predicate<? super AisPacket> f = getSourceFilter(info);
        if (f != Predicate.TRUE) {
            s = s.filter(f);
        }

        String limit = getOneOrZeroParametersOrFail(info, "limit", null);
        if (limit != null) {
            s = s.limit(Long.parseLong(limit));
        }
        // TODO apply user filter

        return s;
    }

    /** Returns a live stream of all incoming data. */
    @GET
    @Path("/stream")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput stream(@Context final UriInfo info) {
        return new StreamingOutput() {
            public void write(final OutputStream os) throws IOException {
                Subscription ss = createStream(info).subscribeSink(getOutputSink(info).newFlushEveryTimeSink(), os);
                // Since this is an infinite stream. We await for the user to cancel the subscription.
                // For example, by killing the process (curl, wget, ..) they are using to retrieve the data with
                // in which the case AisPacketStream.CANCEL will be thrown and awaitCancelled will be released
                try {
                    ss.awaitCancelled();
                } catch (InterruptedException ignore) {} finally {
                    ss.cancel(); // just in case an InterruptedException is thrown
                }
            }
        };
    }
}

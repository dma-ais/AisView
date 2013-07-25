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

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
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
        AisPacketStream s = getViewer().stream();

        Predicate<? super AisPacket> f = getFilter(info);
        if (f != Predicate.TRUE) {
            s = s.filter(f);
        }

        String limit = getOneOr(info, "limit", null);
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
    public StreamingOutput stream(final @Context UriInfo info) {
        return new StreamingOutput() {
            public void write(final OutputStream os) throws IOException, WebApplicationException {
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

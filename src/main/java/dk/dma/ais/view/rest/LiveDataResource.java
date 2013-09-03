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
package dk.dma.ais.view.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.packet.AisPacketStream;
import dk.dma.ais.packet.AisPacketStream.Subscription;
import dk.dma.ais.reader.AisReaderGroup;
import dk.dma.commons.util.io.CountingOutputStream;
import dk.dma.commons.web.rest.AbstractResource;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class LiveDataResource extends AbstractResource {

    /** Returns a live stream of all incoming data. */
    @GET
    @Path("/stream")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput livestream(@Context UriInfo info) {
        final QueryHelper p = new QueryHelper(info);
        return new StreamingOutput() {
            public void write(final OutputStream os) throws IOException {
                AisPacketStream s = LiveDataResource.this.get(AisReaderGroup.class).stream();
                s = p.applySourceFilter(s);
                s = p.applyLimitFilter(s);

                CountingOutputStream cos = new CountingOutputStream(os);
                // We flush the sink after each written line, to be more responsive
                Subscription ss = s.subscribeSink(p.getOutputSink().newFlushEveryTimeSink(), cos);

                // Since this is an infinite stream. We await for the user to cancel the subscription.
                // For example, by killing the process (curl, wget, ..) they are using to retrieve the data with
                // in which the case AisPacketStream.CANCEL will be thrown and awaitCancelled will be released

                // If the user has an expression such as source=id=SDFWER we will never return any data to the
                // client.Therefore we will never try to write any data to the socket.
                // Therefore we will never figure out when the socket it closed. Because we will never get the
                // exception. Instead we close the connection after 24 hours if nothing has been written.
                long lastCount = 0;
                for (;;) {
                    try {
                        if (ss.awaitCancelled(1, TimeUnit.DAYS)) {
                            return;
                        } else if (lastCount == cos.getCount()) {
                            ss.cancel(); // No data written in one day, closing the stream
                        }
                        lastCount = cos.getCount();
                    } catch (InterruptedException ignore) {} finally {
                        ss.cancel(); // just in case an InterruptedException is thrown
                    }
                }
            }
        };
    }
}

/* Copyright (c) 2011 Danish Maritime Authority.
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

    /**
     * @param handler
     */
    public LiveDataResource() {
        super();
    }
    
    private StreamingOutput newStreamingOutput(final QueryParameterHelper p, final UriInfo info) {
        return new StreamingOutput() {
            public void write(final OutputStream os) throws IOException {
                AisPacketStream s = LiveDataResource.this.get(
                        AisReaderGroup.class).stream();
                s = p.applySourceFilter(s);
                s = p.applyLimitFilter(s);

                CountingOutputStream cos = new CountingOutputStream(os);
                // We flush the sink after each written line, to be more
                // responsive
                Subscription ss = s.subscribeSink(p.getOutputSink()
                        .newFlushEveryTimeSink(), cos);

                // Since this is an infinite stream. We await for the user to
                // cancel the subscription.
                // For example, by killing the process (curl, wget, ..) they are
                // using to retrieve the data with
                // in which the case AisPacketStream.CANCEL will be thrown and
                // awaitCancelled will be released

                // If the user has an expression such as source=id=SDFWER we
                // will never return any data to the
                // client.Therefore we will never try to write any data to the
                // socket.
                // Therefore we will never figure out when the socket it closed.
                // Because we will never get the
                // exception. Instead we close the connection after 24 hours if
                // nothing has been written.
                long lastCount = 0;
                for (;;) {
                    try {
                        if (ss.awaitCancelled(1, TimeUnit.DAYS)) {
                            return;
                        } else if (lastCount == cos.getCount()) {
                            ss.cancel(); // No data written in one day, closing
                                         // the stream
                        }
                        lastCount = cos.getCount();
                    } catch (InterruptedException ignore) {
                    } finally {
                        ss.cancel(); // just in case an InterruptedException is
                                     // thrown
                    }
                }
            }
        };
     
    }


    /** Returns a live stream of all incoming data. */
    @GET
    @Path("/stream")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput livestream(@Context UriInfo info) {
        final QueryParameterHelper p = new QueryParameterHelper(info);
        return newStreamingOutput(p, info);
    }

}



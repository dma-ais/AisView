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
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.data.AisVesselPosition;
import dk.dma.ais.data.AisVesselTarget;
import dk.dma.ais.data.IPastTrack;
import dk.dma.ais.data.PastTrackSortedSet;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketStream;
import dk.dma.ais.packet.AisPacketStream.Subscription;
import dk.dma.ais.reader.AisReaderGroup;
import dk.dma.ais.store.AisStoreQueryBuilder;
import dk.dma.ais.tracker.TargetInfo;
import dk.dma.ais.tracker.TargetTracker;
import dk.dma.ais.view.common.web.QueryParams;
import dk.dma.ais.view.handler.AisViewHandler;
import dk.dma.ais.view.handler.TargetSourceData;
import dk.dma.ais.view.rest.json.AnonymousVesselList;
import dk.dma.ais.view.rest.json.BaseVesselList;
import dk.dma.ais.view.rest.json.VesselClusterJsonRepsonse;
import dk.dma.ais.view.rest.json.VesselList;
import dk.dma.ais.view.rest.json.VesselListJsonResponse;
import dk.dma.ais.view.rest.json.VesselTargetDetails;
import dk.dma.commons.util.io.CountingOutputStream;
import dk.dma.commons.web.rest.AbstractResource;
import dk.dma.db.cassandra.CassandraConnection;
import dk.dma.enav.model.geometry.Position;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class LiveDataResource extends AbstractResource {
    private final AisViewHandler handler;

    /**
     * @param handler
     */
    public LiveDataResource() {
        super();

        this.handler = null;
    }
    
    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping(@Context UriInfo info) {
        return "pong";
    }

    /** Returns a live stream of all incoming data. */
    @GET
    @Path("/stream")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput livestream(@Context UriInfo info) {
        final QueryParameterHelper p = new QueryParameterHelper(info);
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

    @GET
    @Path("anon_vessel_list")
    @Produces(MediaType.APPLICATION_JSON)
    public VesselListJsonResponse anonVesselList(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        return vesselList(queryParams, true);
    }

    @GET
    @Path("vessel_list")
    @Produces(MediaType.APPLICATION_JSON)
    public VesselListJsonResponse vesselList(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        return vesselList(queryParams, handler.getConf().isAnonymous());
    }

    @GET
    @Path("vessel_clusters")
    @Produces(MediaType.APPLICATION_JSON)
    public VesselClusterJsonRepsonse vesselClusters(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        return cluster(queryParams);
    }

    @GET
    @Path("vessel_target_details")
    @Produces(MediaType.APPLICATION_JSON)
    public VesselTargetDetails vesselTargetDetails(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        //Integer id = queryParams.getInt("id");
        Integer mmsi = queryParams.getInt("mmsi");
        boolean pastTrack = queryParams.containsKey("past_track");

        // VesselTargetDetails details = handler.getVesselTargetDetails(id,
        // mmsi, pastTrack);
        TargetTracker tt = LiveDataResource.this.get(TargetTracker.class);
        TargetInfo ti = tt.getNewest(mmsi);

        //AisPacketSource aps = AisPacketSource.create(ti.getPositionPacket());
        AisVesselTarget aisVesselTarget;

        try {
            aisVesselTarget = (AisVesselTarget) AisVesselTarget.createTarget(ti
                    .getPositionPacket().getAisMessage());
        } catch (AisMessageException e) {
            // TODO Auto-generated catch block
            throw new WebApplicationException(e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        } catch (SixbitException e) {
            // TODO Auto-generated catch block
            throw new WebApplicationException(e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        TargetSourceData sourceData = new TargetSourceData();
        sourceData.update(ti.getPositionPacket());
        for (AisPacket p : ti.getStaticPackets()) {
            sourceData.update(p);
        }

        IPastTrack pt = new PastTrackSortedSet();
        if (pastTrack) {
            CassandraConnection con = LiveDataResource.this
                    .get(CassandraConnection.class);

            final long timeBack = 1000 * 60 * 60 * 12;
            final long lastPacketTime = ti.getPositionPacket()
                    .getBestTimestamp();

            Iterable<AisPacket> iter = con.execute(AisStoreQueryBuilder
                    .forMmsi(mmsi)
                    .setInterval(lastPacketTime - timeBack, lastPacketTime)
                    .setFetchSize(1000000));

            for (AisPacket p : iter) {
                AisMessage m = p.tryGetAisMessage();
                if (m instanceof IVesselPositionMessage) {
                    AisVesselPosition avp = new AisVesselPosition();
                    avp.update(m);
                    pt.addPosition(avp, 10);
                }
            }
        }

        VesselTargetDetails details = new VesselTargetDetails(aisVesselTarget,
                sourceData, 0, pt);

        return details;
    }

    @GET
    @Path("vessel_search")
    @Produces(MediaType.APPLICATION_JSON)
    public VesselList vesselSearch(@QueryParam("argument") String argument) {
        if (handler.getConf().isAnonymous()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        // Get response from AisViewHandler and return it
        return handler.searchTargets(argument);
    }

    private VesselListJsonResponse vesselList(QueryParams request,
            boolean anonymous) {
        VesselListFilter filter = new VesselListFilter(request);
        // Get corners
        Double topLat = request.getDouble("topLat");
        Double topLon = request.getDouble("topLon");
        Double botLat = request.getDouble("botLat");
        Double botLon = request.getDouble("botLon");

        // Extract requested area
        Position pointA = null;
        Position pointB = null;

        if (topLat != null && topLon != null && botLat != null
                && botLon != null) {
            pointA = Position.create(topLat, topLon);
            pointB = Position.create(botLat, botLon);
        }

        // Get response from AisViewHandler and return it
        BaseVesselList list;
        if (anonymous) {
            list = new AnonymousVesselList();
        } else {
            list = new VesselList();
        }

        // Get request id
        Integer requestId = request.getInt("requestId");
        if (requestId == null) {
            requestId = -1;
        }

        return new VesselListJsonResponse(requestId, handler.getVesselList(
                list, filter, pointA, pointB));
    }

    private VesselClusterJsonRepsonse cluster(QueryParams request) {
        VesselListFilter filter = new VesselListFilter(request);

        // Extract cluster limit
        Integer limit = request.getInt("clusterLimit");
        if (limit == null) {
            limit = 10;
        }

        // Extract cluster size
        Double size = request.getDouble("clusterSize");
        if (size == null) {
            size = 4.0;
        }

        // Get corners
        Double topLat = request.getDouble("topLat");
        Double topLon = request.getDouble("topLon");
        Double botLat = request.getDouble("botLat");
        Double botLon = request.getDouble("botLon");

        // Extract requested area
        Position pointA = null;
        Position pointB = null;

        if (topLat != null && topLon != null && botLat != null
                && botLon != null) {
            pointA = Position.create(topLat, topLon);
            pointB = Position.create(botLat, botLon);
        }

        // Get request id
        Integer requestId = request.getInt("requestId");
        if (requestId == null) {
            requestId = -1;
        }

        return handler.getClusterResponse(requestId, filter, pointA, pointB,
                limit, size);
    }
}

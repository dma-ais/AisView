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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeSet;
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
import dk.dma.ais.data.AisTarget;
import dk.dma.ais.data.AisVesselTarget;
import dk.dma.ais.data.IPastTrack;
import dk.dma.ais.data.PastTrackSortedSet;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.packet.AisPacketSourceFilters;
import dk.dma.ais.packet.AisPacketStream;
import dk.dma.ais.packet.AisPacketStream.Subscription;
import dk.dma.ais.packet.AisPacketTags.SourceType;
import dk.dma.ais.reader.AisReaderGroup;
import dk.dma.ais.store.AisStoreQueryBuilder;
import dk.dma.ais.tracker.TargetInfo;
import dk.dma.ais.tracker.TargetTracker;
import dk.dma.ais.view.common.web.QueryParams;
import dk.dma.ais.view.configuration.AisViewConfiguration;
import dk.dma.ais.view.handler.AisViewHelper;
import dk.dma.ais.view.rest.json.VesselClusterJsonRepsonse;
import dk.dma.ais.view.rest.json.VesselList;
import dk.dma.ais.view.rest.json.VesselListJsonResponse;
import dk.dma.ais.view.rest.json.VesselTargetDetails;
import dk.dma.commons.util.io.CountingOutputStream;
import dk.dma.commons.web.rest.AbstractResource;
import dk.dma.db.cassandra.CassandraConnection;
import dk.dma.enav.model.Country;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.util.function.Predicate;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class LiveDataResource extends AbstractResource {
    private final AisViewHelper handler;

    /**
     * @param handler
     */
    public LiveDataResource() {
        super();

        this.handler = new AisViewHelper(new AisViewConfiguration());
    }

    static final Comparator<AisPacket> COMPARE_TIMESTAMP = new Comparator<AisPacket>() {

        @Override
        public int compare(AisPacket o1, AisPacket o2) {
            // TODO Auto-generated method stub
            return Long.compare(o1.getBestTimestamp(), o2.getBestTimestamp());
        }

    };

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
        return vesselList(queryParams, false);
    }

    @GET
    @Path("vessel_list")
    @Produces(MediaType.APPLICATION_JSON)
    public VesselListJsonResponse vesselList(@Context UriInfo uriInfo) {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        return vesselList(queryParams, false);
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
    public VesselTargetDetails vesselTargetDetails(@Context UriInfo uriInfo) throws Exception {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        Integer mmsi = Objects
                .requireNonNull(queryParams.getInt("mmsi") != null ? queryParams
                        .getInt("mmsi") : queryParams.getInt("id"));
        boolean pastTrack = queryParams.containsKey("past_track");

        // VesselTargetDetails details = handler.getVesselTargetDetails(id,
        // mmsi, pastTrack);
        TargetTracker tt = Objects.requireNonNull(LiveDataResource.this
                .get(TargetTracker.class));
        Objects.requireNonNull(tt.getNewest(mmsi));
        Entry<AisPacketSource, TargetInfo> entry = Objects.requireNonNull(tt
                .getNewestEntry(mmsi));
        TargetInfo ti = entry.getValue();

        

        IPastTrack pt = new PastTrackSortedSet();
        AisVesselTarget aisTarget = null;
        if (pastTrack) {
            CassandraConnection con = LiveDataResource.this
                    .get(CassandraConnection.class);

            final long timeBack = 1000 * 60 * 60 * 24 * 5;
            final long lastPacketTime = entry.getValue().getPositionPacket().getBestTimestamp();

            Iterable<AisPacket> iter = con.execute(AisStoreQueryBuilder
                    .forMmsi(mmsi)
                    .setInterval(lastPacketTime - timeBack, lastPacketTime).setFetchSize(10000));

            for (AisPacket p : iter) {
                AisMessage m = p.tryGetAisMessage();

                if (aisTarget == null) {
                    aisTarget = (AisVesselTarget) AisTarget.createTarget(m);
                } else {
                    aisTarget.update(m);
                }

                if (m instanceof IVesselPositionMessage) {
                    pt.addPosition(aisTarget.getVesselPosition(), handler.getConf().getPastTrackMinDist());
                }
            }
        }
        
        if (aisTarget != null) {
            updateAisTarget(aisTarget, ti);
        } else {
            aisTarget = (AisVesselTarget) generateAisTarget(ti);
        }
         

        VesselTargetDetails details = new VesselTargetDetails(aisTarget,entry.getKey(), mmsi, pt);

        return details;
    }

    @GET
    @Path("vessel_search")
    @Produces(MediaType.APPLICATION_JSON)
    public VesselList vesselSearch(@QueryParam("argument") String argument) {
        if (handler.getConf().isAnonymous()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        TargetTracker tt = LiveDataResource.this.get(TargetTracker.class);

        Map<Integer, TargetInfo> targets = tt.findTargets(Predicate.TRUE,
                Predicate.TRUE);

        LinkedList<AisTarget> aisTargets = new LinkedList<>();

        for (Entry<Integer, TargetInfo> e : targets.entrySet()) {
            AisTarget aisTarget = AisTarget.createTarget(e.getValue()
                    .getPositionPacket().tryGetAisMessage());

            for (AisPacket packet : e.getValue().getStaticPackets()) {
                aisTarget.update(packet.tryGetAisMessage());
                aisTargets.add(aisTarget);
            }
        }

        // Get response from AisViewHandler and return it
        return handler.searchTargets(argument, aisTargets);
    }

    private VesselListJsonResponse vesselList(QueryParams request,
            boolean anonymous) {
        final VesselListFilter filter = new VesselListFilter(request);

        TargetTracker tt = LiveDataResource.this.get(TargetTracker.class);

        BoundingBox bbox = tryGetBbox(request);
        Predicate<? super TargetInfo> targetPredicate = Predicate.TRUE;
        if (bbox != null) {
            targetPredicate = filterOnBoundingBox(bbox);
        }

        // filter both on source and target
        Map<Integer, TargetInfo> targets = tt.findTargets(
                getSourcePredicates(filter), targetPredicate);
        VesselList list = getVesselList(targets, filter);

        int targetCount = tt.countNumberOfTargets(getSourcePredicates(filter),
                targetPredicate);
        list.setInWorldCount(targetCount);

        // Get request id
        Integer requestId = request.getInt("requestId");
        if (requestId == null) {
            requestId = -1;
        }

        return new VesselListJsonResponse(requestId, list);
    }

    private VesselList getVesselList(Map<Integer, TargetInfo> targets,
            VesselListFilter filter) {

        VesselList list = new VesselList();
        for (Entry<Integer, TargetInfo> e : targets.entrySet()) {
            try {
                AisTarget aisTarget = generateAisTarget(e.getValue());
                list.addTarget(handler.getFilteredAisVessel(aisTarget, filter),
                        e.getKey());
            } catch (NullPointerException exc) {
                // pass
            }
        }
        return list;

    }
    
    private TreeSet<AisPacket> getPacketsInOrder(TargetInfo ti) {
        TreeSet<AisPacket> messages = new TreeSet<>(COMPARE_TIMESTAMP);


        for (AisPacket p : ti.getStaticPackets()) {
            try {
                messages.add(Objects.requireNonNull(p));
            } catch (Exception exc) {
                // pass
            }
        }
        
        if (ti.hasPositionInfo()) {
            messages.add(ti.getPositionPacket());
        }
                
        return messages;
    }
    
    /*
    private AisTarget generateAisTarget(TreeSet<AisPacket> messages) {
        return null
    }*/
    
    private AisTarget generateAisTarget(TargetInfo ti) {
        AisTarget aisTarget = null;
        switch (ti.getStaticCount()) {
        case 1:
            aisTarget = AisTarget.createTarget(ti.getStaticPackets()[0].tryGetAisMessage());
            break;
        case 2:
            aisTarget = AisTarget.createTarget(ti.getStaticPackets()[0].tryGetAisMessage());
            aisTarget.update(ti.getStaticPackets()[1].tryGetAisMessage());
            break;
        }
        
        if (ti.hasPositionInfo()) {
            aisTarget.update(ti.getPositionPacket().tryGetAisMessage());
        }
        //return generateAisTarget(getPacketsInOrder(ti));
        return aisTarget;
    }
    
    private AisTarget updateAisTarget(AisTarget aisTarget, TreeSet<AisPacket> messages) {
        for (AisPacket p : messages) {
            try {
                aisTarget.update(p.getAisMessage());
            } catch (IllegalArgumentException | AisMessageException | SixbitException | NullPointerException e) {
                // pass
            }
        }
        return aisTarget;
    }
    
    private AisTarget updateAisTarget(AisTarget aisTarget, TargetInfo ti) {
        return updateAisTarget(aisTarget, getPacketsInOrder(ti));
    }

    private Collection<AisTarget> getAisTargetList(
            Map<Integer, TargetInfo> targets, VesselListFilter filter) {

        ArrayList<AisTarget> list = new ArrayList<>();
        for (Entry<Integer, TargetInfo> e : targets.entrySet()) {
            try {
                AisTarget aisTarget = generateAisTarget(e.getValue());
                list.add(handler.getFilteredAisVessel(aisTarget, filter));
            } catch (NullPointerException e1) {
                // pass
            }

        }

        return list;
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

        BoundingBox bbox = tryGetBbox(request);
        Predicate<? super TargetInfo> targetPredicate = Predicate.TRUE;
        if (bbox != null) {
            targetPredicate = filterOnBoundingBox(bbox);
        }

        Position pointA = Position.create(request.getDouble("topLat"),
                request.getDouble("topLon"));
        Position pointB = Position.create(request.getDouble("botLat"),
                request.getDouble("botLon"));

        TargetTracker tt = LiveDataResource.this.get(TargetTracker.class);
        Map<Integer, TargetInfo> targets = tt.findTargets(
                getSourcePredicates(filter), targetPredicate);

        Collection<AisTarget> aisTargets = getAisTargetList(targets, filter);

        // Get request id
        Integer requestId = request.getInt("requestId");
        if (requestId == null) {
            requestId = -1;
        }

        return handler.getClusterResponse(aisTargets, requestId, filter, limit,
                size, pointA, pointB);
    }

    private Predicate<TargetInfo> filterOnBoundingBox(final BoundingBox bbox) {
        return new Predicate<TargetInfo>() {
            @Override
            public boolean test(TargetInfo arg0) {
                try {
                    return bbox.contains(arg0.getPositionPacket()
                            .getAisMessage().getValidPosition());
                } catch (AisMessageException | SixbitException
                        | NullPointerException e) {
                    return false;
                }
            }
        };
    }

    @SuppressWarnings("unused")
    private Predicate<TargetInfo> filterOnBoundingBox(final Position pointA,
            final Position pointB) {
        return filterOnBoundingBox(BoundingBox.create(pointA, pointB,
                CoordinateSystem.GEODETIC));
    }

    /**
     * Get a Predicate based filter using VesselListFilter
     * 
     * TODO: Replace this with expression parser from QueryParameterHehlper once
     * clients have been updated.
     * 
     * @param key
     * @return
     */
    private Predicate<? super AisPacketSource> getSourcePredicate(
            VesselListFilter filter, String key) {
        Map<String, HashSet<String>> filters = filter.getFilterMap();

        if (filters.containsKey(key)) {
            String[] values = filters.get(key).toArray(new String[0]);

            switch (key) {
            case "sourceCountry":
                return AisPacketSourceFilters.filterOnSourceCountry(Country
                        .findAllByCode(values).toArray(new Country[0]));
            case "sourceRegion":
                return AisPacketSourceFilters.filterOnSourceRegion(values);
            case "sourceBs":
                return AisPacketSourceFilters.filterOnSourceBaseStation(values);
            case "sourceType":
                return AisPacketSourceFilters.filterOnSourceType(SourceType
                        .fromString(filters.get(key).iterator().next()));
            case "sourceSystem":
                return AisPacketSourceFilters.filterOnSourceId(values);
            }
        }

        return Predicate.TRUE;
    }

    /**
     * Get all predicates that relate to source from VesselListFilter.
     * 
     * TODO: Replace this with expression parser from QueryParameterHelper once
     * clients have been updated.
     * 
     * @param filter
     * @return
     */
    private Predicate<? super AisPacketSource> getSourcePredicates(
            final VesselListFilter filter) {
        return new Predicate<AisPacketSource>() {

            @Override
            public boolean test(AisPacketSource arg0) {
                for (String key : filter.getFilterMap().keySet()) {
                    if (!getSourcePredicate(filter, key).test(arg0)) {
                        return false;
                    }

                }

                return true;
            }

        };
    }

    private BoundingBox getBbox(QueryParams request) {
        // Get corners
        Double topLat = request.getDouble("topLat");
        Double topLon = request.getDouble("topLon");
        Double botLat = request.getDouble("botLat");
        Double botLon = request.getDouble("botLon");

        Position pointA = Position.create(topLat, topLon);
        Position pointB = Position.create(botLat, botLon);
        return BoundingBox.create(pointA, pointB, CoordinateSystem.GEODETIC);
    }

    private BoundingBox tryGetBbox(QueryParams request) {
        try {
            return getBbox(request);
        } catch (NullPointerException e) {
            return null;
        }
    }

}

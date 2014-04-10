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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.google.common.cache.Cache;

import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.data.AisTarget;
import dk.dma.ais.data.AisVesselTarget;
import dk.dma.ais.data.IPastTrack;
import dk.dma.ais.data.PastTrackSortedSet;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.packet.AisPacketSourceFilters;
import dk.dma.ais.packet.AisPacketTags.SourceType;
import dk.dma.ais.store.AisStoreQueryBuilder;
import dk.dma.ais.tracker.TargetInfo;
import dk.dma.ais.tracker.TargetTracker;
import dk.dma.ais.view.common.util.CacheManager;
import dk.dma.ais.view.common.web.QueryParams;
import dk.dma.ais.view.configuration.AisViewConfiguration;
import dk.dma.ais.view.handler.AisViewHelper;
import dk.dma.ais.view.rest.json.VesselClusterJsonRepsonse;
import dk.dma.ais.view.rest.json.VesselList;
import dk.dma.ais.view.rest.json.VesselListJsonResponse;
import dk.dma.ais.view.rest.json.VesselTargetDetails;
import dk.dma.commons.web.rest.AbstractResource;
import dk.dma.db.cassandra.CassandraConnection;
import dk.dma.enav.model.Country;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.util.function.Predicate;

/**
 * All previously used web services for backwards compatibility. Ported to use
 * TargetTracker and AisStore functionality
 * 
 * @author Jens Tuxen
 */
@Path("/")
public class LegacyResource extends AbstractResource {
    private final AisViewHelper handler;
    private static final long ONE_DAY = 1000 * 60 * 60 * 24;
    private static final long TEN_MINUTE_BLOCK = 1000 * 60 * 10;

    /**
     * @param handler
     */
    public LegacyResource() {
        super();

        this.handler = new AisViewHelper(new AisViewConfiguration());
    }

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping(@Context UriInfo info) {
        return "pong";
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
    public VesselTargetDetails vesselTargetDetails(@Context UriInfo uriInfo)
            throws Exception {
        QueryParams queryParams = new QueryParams(uriInfo.getQueryParameters());
        final Integer mmsi = Objects
                .requireNonNull(queryParams.getInt("mmsi") != null ? queryParams
                        .getInt("mmsi") : queryParams.getInt("id"));
        boolean pastTrack = queryParams.containsKey("past_track");

        TargetTracker tt = Objects.requireNonNull(LegacyResource.this
                .get(TargetTracker.class));

        Cache<Integer, IPastTrack> cache = LegacyResource.this.get(
                CacheManager.class).getPastTrackCache();

        Entry<AisPacketSource, TargetInfo> entry = Objects.requireNonNull(tt
                .getNewestEntry(mmsi));
        TargetInfo ti = entry.getValue();

        IPastTrack pt = new PastTrackSortedSet();
        final AisVesselTarget aisTarget = (AisVesselTarget) generateAisTarget(ti);

        final double tolerance = 1000;
        final int minDist = 500;

        if (pastTrack) {
            final CassandraConnection con = LegacyResource.this
                    .get(CassandraConnection.class);

            final long mostRecent = ti.getPositionPacket().getBestTimestamp();
            pt = cache.get(mmsi, new Callable<IPastTrack>() {

                @Override
                public IPastTrack call() throws Exception {
                    // TODO Auto-generated method stub
                    return handler.generatePastTrackFromAisStore(aisTarget,
                            mmsi, mostRecent, ONE_DAY * 14, tolerance, minDist,
                            con);
                }

            });

            long recentMissing = mostRecent
                    - pt.getPoints().get(pt.getPoints().size() - 1).getTime();

            // long pastMissing = mostRecent - pt.getPoints().get(0).getTime();

            // 2 minutes need to have passed before we update passtracks
            if (recentMissing > (1000 * 60 * 2)) {
                IPastTrack pt2 = handler.generatePastTrackFromAisStore(
                        aisTarget, mmsi, mostRecent, recentMissing, tolerance,
                        minDist, con);
                pt = handler.combinePastTrack(pt, pt2);
                cache.put(mmsi, pt);
            }

        }

        VesselTargetDetails details = new VesselTargetDetails(aisTarget,
                entry.getKey(), mmsi, pt);

        return details;
    }

    /**
     * Experimental sampled database access
     * 
     * @param start
     * @param stop
     * @param mmsi
     * @return
     */
    @SuppressWarnings("unused")
    private List<AisStoreQueryBuilder> sampledPastTrack(Long start, Long stop,
            int mmsi) {
        if ((stop - start) < TEN_MINUTE_BLOCK) {
            AisStoreQueryBuilder query = AisStoreQueryBuilder.forMmsi(mmsi)
                    .setInterval(start, stop);
            return Arrays.asList(query);
        }

        int startBlock = (int) (start / TEN_MINUTE_BLOCK);
        int numberOfBlocks = (int) ((stop - start) / TEN_MINUTE_BLOCK);

        ArrayList<AisStoreQueryBuilder> list = new ArrayList<>(numberOfBlocks);
        for (int i = 0; i < numberOfBlocks; i++) {
            long sta = (startBlock + i) * TEN_MINUTE_BLOCK;
            long sto = (startBlock + (i + 1)) * TEN_MINUTE_BLOCK;
            list.add(AisStoreQueryBuilder.forMmsi(mmsi)
                    .setInterval(sta, sto - 1).setFetchSize(1000));
        }
        return list;

    }

    @GET
    @Path("vessel_search")
    @Produces(MediaType.APPLICATION_JSON)
    public VesselList vesselSearch(@QueryParam("argument") String argument) {
        if (handler.getConf().isAnonymous()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        TargetTracker tt = LegacyResource.this.get(TargetTracker.class);
        final Predicate<TargetInfo> searchPredicate = getSearchPredicate(argument);

        Map<Integer, TargetInfo> targets = tt.findTargets(Predicate.TRUE,
                searchPredicate);

        LinkedList<AisTarget> aisTargets = new LinkedList<AisTarget>();
        for (Entry<Integer, TargetInfo> e : targets.entrySet()) {
            aisTargets.add(generateAisTarget(e.getValue()));
        }

        // Get response from AisViewHandler and return it
        return handler.searchTargets(argument, aisTargets);
    }

    private VesselListJsonResponse vesselList(QueryParams request,
            boolean anonymous) {
        final VesselListFilter filter = new VesselListFilter(request);

        TargetTracker tt = LegacyResource.this.get(TargetTracker.class);

        BoundingBox bbox = handler.tryGetBbox(request);
        Predicate<? super TargetInfo> targetPredicate = Predicate.TRUE;
        if (bbox != null) {
            targetPredicate = filterOnBoundingBox(bbox);
        }

        // filter both on source and target
        Map<Integer, TargetInfo> targets = tt.findTargets(
                getSourcePredicates(filter), targetPredicate);
        VesselList list = getVesselList(targets, filter);

        list.setInWorldCount(targets.values().size());

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
            AisTarget aisTarget = generateAisTarget(e.getValue());
            AisVesselTarget t = handler.getFilteredAisVessel(aisTarget, filter);

            if (t != null) {
                list.addTarget(t, e.getKey());
            }
        }
        return list;
    }

    private TreeSet<AisPacket> getPacketsInOrder(TargetInfo ti) {
        TreeSet<AisPacket> messages = new TreeSet<>();

        for (AisPacket p : ti.getStaticPackets()) {
            try {
                messages.add(Objects.requireNonNull(p));
            } catch (NullPointerException exc) {
                // pass
            }
        }

        if (ti.hasPositionInfo()) {
            messages.add(ti.getPositionPacket());
        }

        return messages;
    }

    /**
     * Generate AisTarget with first packet being p
     * 
     * @param p
     *            ais packet
     * @param messages
     *            ordered ais packets
     * @return AisTarget
     */
    @SuppressWarnings("unused")
    private AisTarget generateAisTarget(final AisPacket p,
            final TreeSet<AisPacket> messages) {
        AisTarget aisTarget = AisTarget.createTarget(p.tryGetAisMessage());
        return updateAisTarget(aisTarget, messages);
    }

    private AisTarget generateAisTarget(TreeSet<AisPacket> messages) {
        AisTarget aisTarget = null;
        for (AisPacket packet : messages.descendingSet()) {
            aisTarget = AisTarget.createTarget(packet.tryGetAisMessage());

            if (aisTarget != null) {
                break;
            }
        }

        return updateAisTarget(aisTarget, messages);
    }

    private AisTarget generateAisTarget(TargetInfo ti) {
        return generateAisTarget(getPacketsInOrder(ti));
    }

    /**
     * Update aisTarget with messages (note that if the packets are of different
     * class type,
     * 
     * @param aisTarget
     * @param messages
     * @return
     */
    private AisTarget updateAisTarget(AisTarget aisTarget,
            TreeSet<AisPacket> messages) {
        for (AisPacket p : messages.descendingSet()) {
            try {
                aisTarget.update(p.getAisMessage());
            } catch (AisMessageException | SixbitException
                    | NullPointerException e) {
                // pass
            } catch (IllegalArgumentException exc) {
                // happens when we try to update ClassA with ClassB and visa
                // versa
                // the youngest (newest report) takes president
            }
        }
        return aisTarget;
    }

    @SuppressWarnings("unused")
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

        BoundingBox bbox = handler.tryGetBbox(request);
        Predicate<? super TargetInfo> targetPredicate = Predicate.TRUE;
        if (bbox != null) {
            targetPredicate = filterOnBoundingBox(bbox);
        }

        Position pointA = Position.create(request.getDouble("topLat"),
                request.getDouble("topLon"));
        Position pointB = Position.create(request.getDouble("botLat"),
                request.getDouble("botLon"));

        TargetTracker tt = LegacyResource.this.get(TargetTracker.class);
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
                ArrayList<Integer> ints = new ArrayList<>(values.length);
                for (String string : values) {
                    ints.add(Integer.getInteger(string));
                }
                Integer[] integers = (Integer[]) ints.toArray();
                return AisPacketSourceFilters
                        .filterOnSourceBasestation(integers);

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

    private Predicate<TargetInfo> getSearchPredicate(final String searchTerm) {
        return new Predicate<TargetInfo>() {
            @Override
            public boolean test(TargetInfo arg0) {
                return !handler.rejectedBySearchCriteria(
                        generateAisTarget(arg0), searchTerm);
            }
        };
    }

}

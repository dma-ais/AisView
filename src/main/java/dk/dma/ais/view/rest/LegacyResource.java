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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
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

import jsr166e.ConcurrentHashMapV8;
import jsr166e.ConcurrentHashMapV8.Action;

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
    @SuppressWarnings("unused")
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

        //cached method
        //final AisVesselTarget aisTarget = (AisVesselTarget) TargetInfoToAisTarget.generateAisTarget(ti);

        final AisVesselTarget aisTarget = (AisVesselTarget) ti.getAisTarget();

        final double tolerance = 1000;
        final int minDist = 500;

        if (pastTrack) {
            final CassandraConnection con = LegacyResource.this
                    .get(CassandraConnection.class);

            final long mostRecent = ti.getPositionPacket().getBestTimestamp();
            pt = cache.get(mmsi, new Callable<IPastTrack>() {
                @Override
                public IPastTrack call() throws Exception {
                    return handler.generatePastTrackFromAisStore(aisTarget,
                            mmsi, mostRecent, ONE_DAY , tolerance, minDist,
                            con);
                }
            });

            //recentMissing is the missing pasttrack timeframe that is not in the cache
            long recentMissing = mostRecent
                    - pt.getPoints().get(pt.getPoints().size() - 1).getTime();

            // potentially we would need to get missing passtrack from the past
            // long pastMissing = mostRecent - pt.getPoints().get(0).getTime();

            // 2 minutes need to have passed before we update pastracks
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
            aisTargets.add(e.getValue().getAisTarget());
        }

        // Get response from AisViewHandler and return it
        return handler.searchTargets(argument, aisTargets);
    }

    private VesselListJsonResponse vesselList(QueryParams request,
            boolean anonymous) {
        final VesselListFilter filter = new VesselListFilter(request);

        TargetTracker tt = LegacyResource.this.get(TargetTracker.class);

        BoundingBox bbox = handler.tryGetBbox(request);
        Predicate<TargetInfo> targetPredicate = filterOnTTL(handler.getConf()
                .getSatTargetTtl());
        if (bbox != null) {
            targetPredicate = targetPredicate.and(filterOnBoundingBox(bbox));
        }

        // filter both on source and target
        ConcurrentHashMapV8<Integer, TargetInfo> targets = (ConcurrentHashMapV8<Integer, TargetInfo>) tt
                .findTargets(getSourcePredicates(filter), targetPredicate);
        VesselList list = getVesselList(targets, filter);

        list.setInWorldCount(targets.values().size());

        // Get request id
        Integer requestId = request.getInt("requestId");
        if (requestId == null) {
            requestId = -1;
        }

        return new VesselListJsonResponse(requestId, list);
    }

    private VesselList getVesselList(
            ConcurrentHashMapV8<Integer, TargetInfo> targets,
            final VesselListFilter filter) {

        VesselList list = new VesselList();

        for (AisVesselTarget avt : getAisVesselTargetsList(targets, filter)) {
            list.addTarget(avt, avt.getMmsi());
        }

        return list;
    }


    /**
     * Update aisTarget with messages 
     * 
     * @param aisTarget
     * @param messages
     * @return
     */
    static AisTarget updateAisTarget(AisTarget aisTarget,
            TreeSet<AisPacket> messages) {
        for (AisPacket p : messages.descendingSet()) {
            try {
                aisTarget.update(p.getAisMessage());
            } catch (AisMessageException | SixbitException
                    | NullPointerException e) {
                // pass, there was an error in the packet/stream that made the packet unparsable
            } catch (IllegalArgumentException exc) {
                // happens when we try to update ClassA with ClassB and visa
                // versa
                // the youngest (newest report) takes president
            }
        }
        return aisTarget;
    }

    /**
     * This method extracts Collection of AisVesselTarget from TargetTracker. 
     * These collections are used in the older web interface
     * @param targets
     * @param filter
     * @return
     */
    private Collection<AisVesselTarget> getAisVesselTargetsList(
            ConcurrentHashMapV8<Integer, TargetInfo> targets,
            final VesselListFilter filter) {

        final ConcurrentHashMapV8<Integer, AisVesselTarget> avts = new ConcurrentHashMapV8<>();

        targets.forEachValue(10, new Action<TargetInfo>() {

            @Override
            public void apply(TargetInfo arg0) {
                AisVesselTarget avt =
                 handler.getFilteredAisVessel(arg0.getAisTarget(),
                 filter);
                //if TargetInfo has cached AisTarget, we can use that to improve performance drastically
                //AisVesselTarget avt = handler.getFilteredAisVessel(
                //        arg0.getAisTarget(), filter);
                if (avt != null) {
                    avts.put(avt.getMmsi(), avt);
                }
            }
        });

        return avts.values();
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
        Predicate<TargetInfo> targetPredicate = filterOnTTL(handler.getConf()
                .getSatTargetTtl());
        if (bbox != null) {
            targetPredicate = targetPredicate.and(filterOnBoundingBox(bbox));
        }

        Position pointA = Position.create(request.getDouble("topLat"),
                request.getDouble("topLon"));
        Position pointB = Position.create(request.getDouble("botLat"),
                request.getDouble("botLon"));

        TargetTracker tt = LegacyResource.this.get(TargetTracker.class);

        //Long start = System.currentTimeMillis();
        ConcurrentHashMapV8<Integer, TargetInfo> targets = (ConcurrentHashMapV8<Integer, TargetInfo>) tt
                .findTargets(getSourcePredicates(filter), targetPredicate);
        //Long end = System.currentTimeMillis();

        //debug
        //System.out.println("tt.findTargets: " + (end - start) + " ms");

        Collection<AisVesselTarget> aisTargets = getAisVesselTargetsList(
                targets, filter);

        // Get request id
        Integer requestId = request.getInt("requestId");
        if (requestId == null) {
            requestId = -1;
        }

        return handler.getClusterResponse(aisTargets, requestId, filter, limit,
                size, pointA, pointB);
    }

    /**
     * TargetInfo predicate for boundingbox filtering
     * TODO: maybe move to aislib
     * @param bbox
     * @return
     */
    private Predicate<? super TargetInfo> filterOnBoundingBox(
            final BoundingBox bbox) {
        return new Predicate<TargetInfo>() {
            @Override
            public boolean test(TargetInfo arg0) {
                if (arg0.hasPositionInfo() && arg0.getPositionPacket().tryGetPositionTime() != null) {
                    return bbox.contains(arg0.getPositionPacket().tryGetPositionTime());
                }
                return false;
            }
        };
    }

    /**
     * Filter on TTL using AisTarget 
     * This is not generic enough to be moved to aislib. It uses AisTarget generation
     * @param ttl
     * @return
     */
    private Predicate<TargetInfo> filterOnTTL(final int ttl) {
        return new Predicate<TargetInfo>() {
            @Override
            public boolean test(TargetInfo arg0) {
                return arg0.getAisTarget().isAlive(ttl);
            }

        };
    }

    /**
     * Overloaded bounding box filtering using points
     * @param pointA
     * @param pointB
     * @return
     */
    @SuppressWarnings("unused")
    private Predicate<? super TargetInfo> filterOnBoundingBox(
            final Position pointA, final Position pointB) {
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
    private Predicate<AisPacketSource> getSourcePredicate(
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

        return null;
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
    private Predicate<AisPacketSource> getSourcePredicates(
            final VesselListFilter filter) {

        ArrayList<Predicate<AisPacketSource>> predFilters = new ArrayList<>();
        for (String key : filter.getFilterMap().keySet()) {
            Predicate<AisPacketSource> p = getSourcePredicate(filter, key);
            if (p != null) {
                predFilters.add(p);
            }
        }

        Predicate<AisPacketSource> finalPredicate = null;
        for (Predicate<AisPacketSource> p : predFilters) {
            if (finalPredicate == null) {
                finalPredicate = p;
            } else {
                finalPredicate = finalPredicate.and(p);
            }

        }

        //pick the finalpredicate if it's not null or create an equivalent one to Predicate.TRUE
        finalPredicate = (finalPredicate == null) ? new Predicate<AisPacketSource>() {

            @Override
            public boolean test(AisPacketSource arg0) {
                return true;
            }
        }
                : finalPredicate;

        return finalPredicate;
    }

    /**
     * Use code from AisViewHelper and AisTarget + reflection to filter on a vague search term
     * The search term will match name, mmsi and other fields.
     * @param searchTerm
     * @return
     */
    private Predicate<TargetInfo> getSearchPredicate(final String searchTerm) {
        return new Predicate<TargetInfo>() {
            @Override
            public boolean test(TargetInfo arg0) {
                return !handler.rejectedBySearchCriteria(
                        arg0.getAisTarget(), searchTerm);
            }
        };
    }

}

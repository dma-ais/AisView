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

import com.google.common.cache.Cache;
import dk.dma.ais.data.AisTarget;
import dk.dma.ais.data.AisVesselTarget;
import dk.dma.ais.data.IPastTrack;
import dk.dma.ais.data.PastTrackSortedSet;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.packet.AisPacketSourceFilters;
import dk.dma.ais.packet.AisPacketTags.SourceType;
import dk.dma.ais.tracker.targetTracker.TargetInfo;
import dk.dma.ais.tracker.targetTracker.TargetInfoToAisTarget;
import dk.dma.ais.tracker.targetTracker.TargetTracker;
import dk.dma.ais.view.common.util.CacheManager;
import dk.dma.ais.view.common.util.TargetInfoFilters;
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
import dk.dma.enav.model.geometry.Position;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /** */
    public LegacyResource() {
        this.handler = new AisViewHelper(new AisViewConfiguration());
    }

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping(@Context UriInfo info) {
        return "pong";
    }

    @GET
    @Path("rate")
    @Produces(MediaType.TEXT_PLAIN)
    public String rate(@QueryParam("expected") Double expected) {
        if (expected == null) {
            expected = 0.0;
        }

        Long r = this.rateCount(null);
        return "status=" + (r > expected ? "ok" : "nok");
    }

    @GET
    @Path("rate/count")
    @Produces(MediaType.TEXT_PLAIN)
    public Long rateCount(@Context UriInfo uriInfo) {
        TargetTracker tt = Objects.requireNonNull(LegacyResource.this
                .get(TargetTracker.class));

        Stream<TargetInfo> tis = tt.stream();

        final Date d = new Date(new Date().getTime() - 1000);
        final Long dLong = d.getTime();

        // count all packets in the TargetInfo which are after the given
        // timestamp.
        // this is kind of "double work" since we already filtered them above.
        Long r = tis.mapToLong(value -> {

            long r1 = 0L;
            if (value.getPositionTimestamp() > dLong) {
                r1++;
            }

            // eep, this is slow but has to be done
            if (value.getStaticCount() > 1) {
                for (AisPacket p : value.getStaticPackets()) {
                    if (p.getBestTimestamp() > dLong) {
                        r1++;
                    }
                }
            } else if (value.getStaticTimestamp() > dLong) {
                r1++;
            }

            return r1;
        }).sum();

        return new Double((double) r).longValue();
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

        TargetInfo ti = tt.get(mmsi);
        
        AisVesselTarget target = (AisVesselTarget)TargetInfoToAisTarget
                .generateAisTarget(ti);

        IPastTrack pt = new PastTrackSortedSet();

        final double tolerance = 1000;
        final int minDist = 500;

        // TODO: make Cassandra totally optional
        // workaround for no cassandra connection
        CassandraConnection connection = null;
        try {
            connection = LegacyResource.this.get(CassandraConnection.class);
        } catch (UnsupportedOperationException e) {

        }

        if (pastTrack && connection != null) {
            final CassandraConnection con = connection;

            final long mostRecent = ti.getPositionPacket().getBestTimestamp();
            pt = cache.get(mmsi, new Callable<IPastTrack>() {
                @Override
                public IPastTrack call() throws Exception {
                    return handler.generatePastTrackFromAisStore(mmsi,
                            mostRecent, ONE_DAY, tolerance, minDist, con);
                }
            });

            // recentMissing is the missing pasttrack timeframe that is not in
            // the cache
            long recentMissing = mostRecent
                    - pt.getPoints().get(pt.getPoints().size() - 1).getTime();

            // potentially we would need to get missing passtrack from the past
            // long pastMissing = mostRecent - pt.getPoints().get(0).getTime();

            // 2 minutes need to have passed before we update pastracks
            if (recentMissing > (1000 * 60 * 2)) {
                IPastTrack pt2 = handler.generatePastTrackFromAisStore(mmsi,
                        mostRecent, recentMissing, tolerance, minDist, con);
                pt = handler.combinePastTrack(pt, pt2);
                cache.put(mmsi, pt);
            }

        }

        VesselTargetDetails details = new VesselTargetDetails(target,
                ti.getPacketSource(), mmsi, pt);

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

        List<AisTarget> aisTargets = tt.stream(e -> true, searchPredicate).map(ti -> ti.getAisTarget()).collect(Collectors.toList());

        // Get response from AisViewHandler and return it
        return handler.searchTargets(argument, aisTargets);
    }

    private VesselListJsonResponse vesselList(QueryParams request,
            boolean anonymous) {
        final VesselListFilter filter = new VesselListFilter(request);

        TargetTracker tt = LegacyResource.this.get(TargetTracker.class);

        BoundingBox bbox = handler.tryGetBbox(request);
        Predicate<TargetInfo> targetPredicate = bbox != null ? TargetInfoFilters.filterOnBoundingBox(bbox) : e->true;

        targetPredicate.and(TargetInfoFilters.filterOnTTL(handler.getConf().getLiveTargetTtl()));
       
        targetPredicate = targetPredicate.and(getTargetPredicates(filter));

        Stream<TargetInfo>targets = tt.stream(getSourcePredicates(filter), targetPredicate);
        
        VesselList list = new VesselList();
        targets.parallel().forEach(e -> list.addTarget(e, e.getMmsi()));
        
        // get count for all in world with source predicates.
        list.setInWorldCount(tt.size());

        // Get request id
        Integer requestId = request.getInt("requestId");
        if (requestId == null) {
            requestId = -1;
        }

        return new VesselListJsonResponse(requestId, list);
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
        //
        Predicate<TargetInfo> targetPredicate = bbox != null ? TargetInfoFilters.filterOnBoundingBox(bbox)
                : e -> true;

        targetPredicate = targetPredicate.and(TargetInfoFilters.filterOnTTL(handler.getConf().getLiveTargetTtl()));
        targetPredicate = targetPredicate.and(getTargetPredicates(filter));

        Position pointA = Position.create(request.getDouble("topLat"),
                request.getDouble("topLon"));
        Position pointB = Position.create(request.getDouble("botLat"),
                request.getDouble("botLon"));

        TargetTracker tt = LegacyResource.this.get(TargetTracker.class);

        Stream<TargetInfo> targets = tt.stream(
                getSourcePredicates(filter), targetPredicate);
        
        // Get request id
        Integer requestId = request.getInt("requestId");
        if (requestId == null) {
            requestId = -1;
        }

        return handler.getClusterResponse(targets, requestId, limit,
                size, pointA, pointB, tt.size());
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

    private Predicate<TargetInfo> getTargetPredicates(
            final VesselListFilter filter) {
        

        List<Predicate<TargetInfo>> preds = filter.getFilterMap().keySet()
                .stream().map(e -> getTargetPredicate(filter, e))
                .filter(e -> e != null).collect(Collectors.toList());

        Optional<Predicate<TargetInfo>> finalPredicate = preds.stream().reduce((a, b) -> a.and(b));

        return (finalPredicate.isPresent()) ? finalPredicate.get() : e->true;
    }
    
    /**
     * Get all predicates that relate to source from VesselListFilter.
     * 
     * TODO: Replace this with expression parser from QueryParameterHelper once
     * clients have been updated.
     * 
     * @param filter
     * @return a collection of .and predicates, or an always true predicate 
     */
    private Predicate<AisPacketSource> getSourcePredicates(
            final VesselListFilter filter) {

        List<Predicate<AisPacketSource>> preds = filter.getFilterMap().keySet()
                .stream().map(e -> getSourcePredicate(filter, e))
                .filter(e -> e != null).collect(Collectors.toList());

        Optional<Predicate<AisPacketSource>> finalPredicate = preds.stream().reduce((a, b) -> a.and(b));

        return (finalPredicate.isPresent()) ? finalPredicate.get() : e->true;
    }    

    /**
     * maps legacy VesselListFilter to predicate based ones for TargetInfo
     * @param filter
     * @param key
     * @return
     */
    private Predicate<TargetInfo> getTargetPredicate(VesselListFilter filter,
            String key) {

        Map<String, HashSet<String>> filters = filter.getFilterMap();
        

        if (filters.containsKey(key)) {
            String[] values = filters.get(key).toArray(new String[0]);

            switch (key) {
            case "country":
                List<Country> countries = Country.findAllByCode(values);
                return e -> countries.stream().sequential()
                        .anyMatch(x -> x == e.getCountry());
            case "staticReport":
                return TargetInfoFilters.filterOnHasStatic();
            }

        }

        return null;
    }

    /**
     * Use code from AisViewHelper and AisTarget + reflection to filter on a
     * vague search term The search term will match name, mmsi and other fields.
     * 
     * @param searchTerm
     * @return
     */
    private Predicate<TargetInfo> getSearchPredicate(final String searchTerm) {
        return ti -> !handler.rejectedBySearchCriteria(ti.getAisTarget(), searchTerm);
    }

}

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

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.ArrayUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.packet.AisPacketFiltersStateful;
import dk.dma.ais.packet.AisPacketOutputSinks;
import dk.dma.ais.store.AisStoreQueryBuilder;
import dk.dma.ais.store.AisStoreQueryResult;
import dk.dma.ais.store.job.JobManager;
import dk.dma.commons.util.Iterables;
import dk.dma.commons.util.JSONObject;
import dk.dma.commons.util.io.OutputStreamSink;
import dk.dma.commons.web.rest.AbstractResource;
import dk.dma.commons.web.rest.StreamingUtil;
import dk.dma.commons.web.rest.query.QueryParameterValidators;
import dk.dma.db.cassandra.CassandraConnection;
import dk.dma.enav.model.geometry.BoundingBox;

/**
 * Resources that query AisStore.
 * 
 * @author Kasper Nielsen
 * @author Thomas Borg Salling
 * @author Jens Tuxen
 */
@Path("/store")
public class AisStoreResource extends AbstractResource {
    

    private static final Logger LOG = LoggerFactory
            .getLogger(AisStoreResource.class);

    static {
        LOG.debug("StatisticDataRepositoryMapDB loaded.");
    }

    {
        LOG.debug(getClass().getSimpleName() + " created (" + this + ").");
    }

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping(@Context UriInfo info) {
        return "pong";
    }

    /**
     * Returns a list of source IDs in the source. This one is hard coded for
     * now
     * 
     * @return a list of source IDs in the source
     */
    @GET
    @Path("/sourceIDs")
    public JSONObject getSourceIDs() {
        return JSONObject.singleList("sourceIDs", "AISD", "IALA", "AISSAT",
                "MSSIS", "AISHUB");
    }

    /**
     * Get the count of messages from packets received with timestamp in the
     * closest full 10 minutes time block.
     * 
     * @return
     */
    private AtomicLong getTenMinuteCount() {
        // get the latest guaranteed full block
        long startBlock = (long) ((double) DateTime.now().getMillis() / 10.0 / 60.0 / 1000.0) - 1;
        long endBlock = startBlock + 1;

        long start = startBlock * 10 * 60 * 1000;
        long end = endBlock * 10 * 60 * 1000;

        AisStoreQueryBuilder b = AisStoreQueryBuilder.forTime().setInterval(
                start, end);
        AisStoreQueryResult query = get(CassandraConnection.class).execute(b);
        Iterable<AisPacket> q = query;

        final AtomicLong l = new AtomicLong();
        q = Iterables.counting(q, l);
        for (Iterator<AisPacket> iterator = q.iterator(); iterator.hasNext();) {
            iterator.next();
        }

        return l;
    }

    /**
     * 
     * @return
     */
    @GET
    @Path("/count")
    public Long getTenMinuteCount(@Context UriInfo info) {
        return getTenMinuteCount().get();
    }

    /**
     * 
     * @return
     */
    @GET
    @Path("/count/second")
    public Double getPacketsPerSecond() {
        return getTenMinuteCount().doubleValue() / 600;
    }

    /**
     * 
     * @return
     */
    @GET
    @Path("/count/minute")
    public Double getPacketsPerMinute() {
        return getTenMinuteCount().doubleValue() / 10;
    }

    /**
     * Check against the expected rate of packets. Just like legacy /rate this
     * is a check for packets seen on average the last 10 minutes.
     * 
     * @param expected
     *            number of packets expected every second (e.g 700)
     * @return "status=nok" or "status=ok"
     */
    @GET
    @Path("rate")
    @Produces(MediaType.TEXT_PLAIN)
    public String rate(@QueryParam("expected") Double expected) {
        if (expected == null) {
            expected = 0.0;
        }

        Double r = this.getTenMinuteCount().doubleValue() / 600.0;
        return "status=" + (r.intValue() > expected ? "ok" : "nok");
    }

    @GET
    @Produces("application/octet-stream")
    @Path("/query")
    public StreamingOutput query(@Context UriInfo info) {
        QueryParameterHelper p = new QueryParameterHelper(info);

        // Create builder, we first need to determine which of the 3 AisStore
        // tables we need to use
        AisStoreQueryBuilder b;
        if (p.getMMSIs().length > 0) {
            b = AisStoreQueryBuilder.forMmsi(p.getMMSIs());
            b.setFetchSize(QueryParameterValidators.getParameterAsInt(info,
                    "fetchSize", 3000));

        /*    
        } else if (p.getArea() != null) {
            b = AisStoreQueryBuilder.forArea(p.getArea());
            b.setFetchSize(QueryParameterValidators.getParameterAsInt(info,
                    "fetchSize", 200));
        */

        } else {
            b = AisStoreQueryBuilder.forTime();
            b.setFetchSize(QueryParameterValidators.getParameterAsInt(info,
                    "fetchSize", 3000));
        }
        // Set various properties for the query builder

        b.setInterval(p.getInterval());

        // Create the query
        AtomicLong counter = new AtomicLong();
        AisStoreQueryResult query = get(CassandraConnection.class).execute(b);
        Iterable<AisPacket> q = query;
        // Apply filters from the user
        // Apply area filter again, problem with position tagging of static data

        final AisPacketFiltersStateful state = new AisPacketFiltersStateful();
        q = p.applySourceFilter(q);
        q = p.applyTargetFilterArea(q, state);
        q = p.applyLimitFilter(q); // WARNING: Must be the last filter (if other
                                   // filters reject packets)

        q = Iterables.counting(q, counter);
        if (p.jobId != null) {
            get(JobManager.class).addJob(p.jobId, query, counter);
        }
        return StreamingUtil.createStreamingOutput(q, p.getOutputSink(), query);
    }

    /*
     * Example URL:
     * http://localhost:8090/store/track/219014434?interval=2014-04-
     * 23&limit=2000000
     */
    @GET
    @Path("/track/{mmsi : \\d+}")
    @Produces("application/json")
    public StreamingOutput pastTrack(@Context UriInfo info,
            @PathParam("mmsi") int mmsi) {
        Iterable<AisPacket> query = getPastTrack(info, mmsi);
        return StreamingUtil.createStreamingOutput(query,
                AisPacketOutputSinks.PAST_TRACK_JSON);
    }

    /*
     * Example URL:
     * http://localhost:8090/store/track?mmsi=219014434&mmsi=219872000
     * &interval=2014-04-23&limit=200000
     */
    @GET
    @Path("/track")
    @Produces("application/octet-stream")
    public StreamingOutput pastTrack(@Context UriInfo info,
            @QueryParam("mmsi") List<Integer> mmsis) {
        Iterable<AisPacket> query = getPastTrack(
                info,
                ArrayUtils.toPrimitive(mmsis.toArray(new Integer[mmsis.size()])));
        return StreamingUtil.createStreamingOutput(query,
                AisPacketOutputSinks.PAST_TRACK_JSON);
    }

    @GET
    @Path("/track/raw/{mmsi : \\d+}")
    @Produces("application/octet-stream")
    public StreamingOutput pastTrackRaw(@Context UriInfo info,
            @PathParam("mmsi") int mmsi) {
        Iterable<AisPacket> query = getPastTrack(info, mmsi);
        return StreamingUtil.createStreamingOutput(query,
                AisPacketOutputSinks.OUTPUT_TO_TEXT);
    }

    @GET
    @Path("/track/raw")
    @Produces("application/octet-stream")
    public StreamingOutput pastTrackRaw(@Context UriInfo info,
            @QueryParam("mmsi") List<Integer> mmsis) {
        Iterable<AisPacket> query = getPastTrack(
                info,
                ArrayUtils.toPrimitive(mmsis.toArray(new Integer[mmsis.size()])));
        return StreamingUtil.createStreamingOutput(query,
                AisPacketOutputSinks.OUTPUT_TO_TEXT);
    }

    @GET
    @Path("/track/html")
    @Produces("text/html")
    public StreamingOutput pastTrackHtml(@Context UriInfo info,
            @QueryParam("mmsi") List<Integer> mmsis) {
        Iterable<AisPacket> query = getPastTrack(
                info,
                ArrayUtils.toPrimitive(mmsis.toArray(new Integer[mmsis.size()])));
        return StreamingUtil.createStreamingOutput(query,
                AisPacketOutputSinks.OUTPUT_TO_HTML);
    }

    @GET
    @Path("/track/kml")
    @Produces(MEDIA_TYPE_KMZ)
    public Response pastTrackKml(@Context UriInfo info,
            @QueryParam("mmsi") List<Integer> mmsis) {
        Iterable<AisPacket> query = getPastTrack(
                info,
                ArrayUtils.toPrimitive(mmsis.toArray(new Integer[mmsis.size()])));
        return Response
                .ok()
                .entity(StreamingUtil.createZippedStreamingOutput(query,
                        AisPacketOutputSinks.newKmlSink(), "track.kml"))
                .type(MEDIA_TYPE_KMZ).build();
    }

    @GET
    @Path("/track/prefixed")
    @Produces("application/octet-stream")
    public StreamingOutput pastTrackPrefixed(@Context UriInfo info,
            @QueryParam("mmsi") List<Integer> mmsis) {
        Iterable<AisPacket> query = getPastTrack(
                info,
                ArrayUtils.toPrimitive(mmsis.toArray(new Integer[mmsis.size()])));
        return StreamingUtil.createStreamingOutput(query,
                AisPacketOutputSinks.OUTPUT_PREFIXED_SENTENCES);
    }

    @GET
    @Path("/history")
    @Produces("application/octet-stream")
    public StreamingOutput history(@Context UriInfo info,
            @QueryParam("mmsi") List<Integer> mmsis) {
        return historyRaw(info, mmsis);
    }

    @GET
    @Path("/history/raw")
    @Produces("application/octet-stream")
    public StreamingOutput historyRaw(@Context UriInfo info,
            @QueryParam("mmsi") List<Integer> mmsis) {
        Iterable<AisPacket> query = getHistory(
                info,
                ArrayUtils.toPrimitive(mmsis.toArray(new Integer[mmsis.size()])));
        return StreamingUtil.createStreamingOutput(query,
                AisPacketOutputSinks.OUTPUT_TO_TEXT);
    }

    @GET
    @Path("/history/html")
    @Produces("text/html")
    public StreamingOutput historyHtml(@Context UriInfo info,
            @QueryParam("mmsi") List<Integer> mmsis) {
        Iterable<AisPacket> query = getHistory(
                info,
                ArrayUtils.toPrimitive(mmsis.toArray(new Integer[mmsis.size()])));
        return StreamingUtil.createStreamingOutput(query,
                AisPacketOutputSinks.OUTPUT_TO_HTML);
    }

    @GET
    @Path("/history/prefixed")
    @Produces("application/octet-stream")
    public StreamingOutput historyPrefixed(@Context UriInfo info,
            @QueryParam("mmsi") List<Integer> mmsis) {
        Iterable<AisPacket> query = getHistory(
                info,
                ArrayUtils.toPrimitive(mmsis.toArray(new Integer[mmsis.size()])));
        return StreamingUtil.createStreamingOutput(query,
                AisPacketOutputSinks.OUTPUT_PREFIXED_SENTENCES);
    }

    @GET
    @Path("/history/kml")
    @Produces(MEDIA_TYPE_KMZ)
    public Response historyKml(@Context UriInfo info,
            @QueryParam("mmsi") List<Integer> mmsis) {
        Iterable<AisPacket> query = getHistory(
                info,
                ArrayUtils.toPrimitive(mmsis.toArray(new Integer[mmsis.size()])));
        return Response
                .ok()
                .entity(StreamingUtil.createZippedStreamingOutput(query,
                        AisPacketOutputSinks.newKmlSink(), "history.kml"))
                .type(MEDIA_TYPE_KMZ).build();
    }

    /**
     * Extract a scenario from AisStore and return it in KML format. Intended
     * for scenario replay sessions using Google Earth.
     *
     * A scenario is a set of vessels and movements constrained by geographical
     * bounding box, time interval, and optionally mmsi no.s. It is possible to
     * include custom data in the generated KML; e.g. scenario title and
     * description.
     *
     * Example URLs: -
     * http://localhost:8090/store/scenario?box=56.12,11.10,56.13
     * ,11.09&interval=2014-04-23
     */
    @GET
    @Path("/scenario")
    @Produces(MEDIA_TYPE_KMZ)
    public Response scenarioKmlGet(@Context UriInfo info) {
        final QueryParameterHelper p = new QueryParameterHelper(info);
        requireNonNull(p.getArea(), "Missing box parameter.");
        return scenarioKmz(p.area, p.interval, p.title, p.description,
                p.createSituationFolder, p.createMovementsFolder,
                p.createTracksFolder, p.primaryMmsi, p.secondaryMmsi,
                p.kmlSnapshotAt, p.interpolationStepSecs);
    }

    /**
     * Search data from AisStore and generate KMZ output.
     * 
     * @param area
     *            extract AisPackets from AisStore inside this area.
     * @param interval
     *            extract AisPackets from AisStore inside this time interval.
     * @param title
     *            Stamp this title into the generated KML (optional).
     * @param description
     *            Stamp this description into the generated KML (optional).
     * @param primaryMmsi
     *            Style this MMSI as the primary target in the scenario
     *            (optional).
     * @param secondaryMmsi
     *            Style this MMSI as the secondary target in the scenario
     *            (optional).
     * @param snapshotAt
     *            Generate a KML snapshot folder for exactly this point in time
     *            (optional).
     * @param interpolationStepSecs
     *            Interpolate targets between AisPackets using this time step in
     *            seconds (optional).
     * @return HTTP response carrying KML for Google Earth
     */
    @SuppressWarnings("unchecked")
    private Response scenarioKmz(final BoundingBox area,
            final Interval interval, final String title,
            final String description, final boolean createSituationFolder,
            final boolean createMovementsFolder,
            final boolean createTracksFolder, final Integer primaryMmsi,
            final Integer secondaryMmsi, final DateTime snapshotAt,
            final Integer interpolationStepSecs) {

        // Pre-check input
        final Duration duration = interval.toDuration();
        final long hours = duration.getStandardHours();
        final long minutes = duration.getStandardMinutes();
        if (hours > 6) {
            throw new IllegalArgumentException(
                    "Queries spanning more than 6 hours are not allowed.");
        }

        final float size = area.getArea();
        if (size > 2500.0 * 1e6) {
            throw new IllegalArgumentException(
                    "Queries spanning more than 2500 square kilometers are not allowed.");
        }

        LOG.info("Preparing KML for span of " + hours + " hours + " + minutes
                + " minutes and " + (float) size + " square kilometers.");

        // Create the query
        AisStoreQueryBuilder b = AisStoreQueryBuilder.forTime(); // Cannot use
                                                                 // getArea
                                                                 // because this
                                                                 // removes all
                                                                 // type 5
        b.setInterval(interval);

        // Execute the query
        AisStoreQueryResult queryResult = get(CassandraConnection.class)
                .execute(b);

        // Apply filters
        Iterable<AisPacket> filteredQueryResult = Iterables.filter(queryResult,
                AisPacketFilters.filterOnMessageId(1, 2, 3, 5, 18, 19, 24));
        filteredQueryResult = Iterables.filter(filteredQueryResult,
                AisPacketFilters.filterRelaxedOnMessagePositionWithin(area));

        if (!filteredQueryResult.iterator().hasNext()) {
            LOG.warn("No AIS data matching criteria.");
        }
        
        Predicate<? super AisPacket> isPrimaryMmsi = primaryMmsi == null ? e->true
                : aisPacket->aisPacket.tryGetAisMessage().getUserId() == primaryMmsi.intValue();

        Predicate<? super AisPacket> isSecondaryMmsi =  secondaryMmsi == null ? e->true
                : aisPacket->aisPacket.tryGetAisMessage().getUserId() == secondaryMmsi.intValue();

        Predicate<? super AisPacket> triggerSnapshot =  snapshotAt != null ? new Predicate<AisPacket>() {
            private final long snapshotAtMillis = snapshotAt.getMillis();
            private boolean snapshotGenerated;

            @Override
            public boolean test(AisPacket aisPacket) {
                boolean generateSnapshot = false;
                if (!snapshotGenerated) {
                    if (aisPacket.getBestTimestamp() >= snapshotAtMillis) {
                        generateSnapshot = true;
                        snapshotGenerated = true;
                    }
                }
                return generateSnapshot;
            }
        }
                : e->true;

        Supplier<? extends String> supplySnapshotDescription = ()->{ return "<table width=\"300\"><tr><td><h4>" + title
                        + "</h4></td></tr><tr><td><p>" + description + "</p></td></tr></table>";};

        Supplier<? extends String> supplyTitle = title != null ? ()->title : null;

        Supplier<? extends String> supplyDescription = description != null ? ()->description: null;

        Supplier<? extends Integer> supplyInterpolationStep = interpolationStepSecs != null ? ()->interpolationStepSecs : null;

        final OutputStreamSink<AisPacket> kmzSink = AisPacketOutputSinks
                .newKmzSink(e->true, createSituationFolder,
                        createMovementsFolder, createTracksFolder,
                        isPrimaryMmsi, isSecondaryMmsi, triggerSnapshot,
                        supplySnapshotDescription, supplyInterpolationStep,
                        supplyTitle, supplyDescription, null);

        return Response
                .ok()
                .entity(StreamingUtil.createStreamingOutput(
                        filteredQueryResult, kmzSink))
                .type(MEDIA_TYPE_KMZ)
                .header("Content-Disposition",
                        "attachment; filename = \"scenario.kmz\"").build();
    }

    /**
     * getPastTrack will only work with position messages
     * 
     * @param info
     * @param mmsi
     * @return
     */
    private Iterable<AisPacket> getPastTrack(@Context UriInfo info, int... mmsi) {
        QueryParameterHelper p = new QueryParameterHelper(info);

        // Execute the query
        AisStoreQueryBuilder b = AisStoreQueryBuilder.forMmsi(mmsi);
        b.setInterval(p.getInterval());

        // Create the query
        Iterable<AisPacket> query = get(CassandraConnection.class).execute(b);

        // Apply filters from the user
        query = Iterables.filter(query, AisPacketFilters
                .filterOnMessageType(IVesselPositionMessage.class));
        query = p.applySourceFilter(query);
        query = p.applyPositionSampler(query); // WARNING: Must be the second
                                               // last filter
        query = p.applyLimitFilter(query); // WARNING: Must be the last filter
                                           // (if other filters reject packets)
        return query;
    }

    /**
     * getHistory takes all ais data with mmsis using stateful filters
     * 
     * @param info
     * @param mmsi
     * @return
     */
    private Iterable<AisPacket> getHistory(@Context UriInfo info, int... mmsi) {
        QueryParameterHelper p = new QueryParameterHelper(info);

        // Execute the query
        AisStoreQueryBuilder b = AisStoreQueryBuilder.forMmsi(mmsi);
        b.setInterval(p.getInterval());

        // Create the query
        Iterable<AisPacket> query = get(CassandraConnection.class).execute(b);

        final AisPacketFiltersStateful state = new AisPacketFiltersStateful();

        // Apply filters from the user
        query = p.applySourceFilter(query); // first because this potentially
                                            // filters a lot of packets
        query = p.applyTargetFilterArea(query, state);
        query = p.applyLimitFilter(query); // WARNING: Must be the last filter
                                           // (if other filters reject packets)
        return query;
    }

    private static final String MEDIA_TYPE_KMZ = "application/vnd.google-earth.kmz";

}

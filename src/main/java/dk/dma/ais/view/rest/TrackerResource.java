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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFiltersStateful;
import dk.dma.ais.packet.AisPacketOutputSinks;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.tracker.TargetInfo;
import dk.dma.ais.tracker.TargetTracker;
import dk.dma.ais.view.common.util.TargetInfoFilters;
import dk.dma.commons.web.rest.AbstractResource;
import dk.dma.commons.web.rest.StreamingUtil;

/**
 * 
 * @author Jens Tuxen
 */
@Path("/tracker")
public class TrackerResource extends AbstractResource {

    /**
     * @param handler
     */
    public TrackerResource() {
        super();
    }
    
    
    @GET
    @Path("/count/targetinfo")
    @Produces(MediaType.TEXT_PLAIN)
    public int getTargetInfoCount(@Context UriInfo info) {
        return get(TargetTracker.class).findTargets(s->true, t->true).size();
    }    

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public int getCount(@Context UriInfo info) {
        return get(TargetTracker.class).countNumberOfTargets(s->true,t->true);
    }

    @GET
    @Path("/static/{mmsi : \\d+}")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput getStaticInfo(@Context UriInfo info,
            @PathParam("mmsi") int mmsi) {
        QueryParameterHelper qh = new QueryParameterHelper(info);

        Predicate<AisPacketSource> pred = qh.getSourcePredicate();
        pred = (pred == null) ? e -> true : pred;

        TargetTracker tt = get(TargetTracker.class);
        TargetInfo ti = tt.getNewest(mmsi, pred);
        List<AisPacket> l = java.util.Arrays.asList(ti.getStaticPackets());

        Iterable<AisPacket> p = applyFilters(l.stream(), qh);
        
        return StreamingUtil.createStreamingOutput(p,
                AisPacketOutputSinks.JSON_MESSAGE);
    }

    @GET
    @Path("/static/")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput getStatics(@Context UriInfo info) {
        QueryParameterHelper qh = new QueryParameterHelper(info);

        Predicate<AisPacketSource> predSource = qh.getSourcePredicate();
        predSource = (predSource == null) ? e -> true : predSource;

        Predicate<TargetInfo> predTarget = qh.getTargetPredicate();
        predTarget = (predTarget == null) ? e -> true : predTarget;
        
        predTarget.and(TargetInfoFilters.filterOnHasStatic());
        predTarget = (qh.getArea() != null) ? qh.getTargetAreaFilter() : predTarget;

        TargetTracker tt = get(TargetTracker.class);

        Stream<TargetInfo> s = tt.findTargets8(predSource, predTarget);

        Stream<AisPacket[]> sPackets = s.map(e -> e.getPackets());

        final ConcurrentLinkedDeque<AisPacket> packets = new ConcurrentLinkedDeque<AisPacket>();
        sPackets.forEach(e -> {
            for (int i = 0; i < e.length; i++) {
                packets.add(e[i]);
            }
        });

        Iterable<AisPacket> p = applyFilters(packets.stream(), qh);

        return StreamingUtil.createStreamingOutput(p,
                AisPacketOutputSinks.JSON_STATIC_LIST);
    }

    @GET
    @Path("/dynamic/{mmsi : \\d+}")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput getPositionInfo(@Context UriInfo info,
            @PathParam("mmsi") int mmsi) {
        QueryParameterHelper qh = new QueryParameterHelper(info);

        Predicate<AisPacketSource> pred = qh.getSourcePredicate();
        pred = (pred == null) ? e -> true : pred;

        TargetTracker tt = get(TargetTracker.class);
        TargetInfo ti = tt.getNewest(mmsi, pred);

        // convert TargetInfo to Stream<AisPacket> and then Iterable<AisPacket>
        // after applying filters
        Iterable<AisPacket> packets = applyFilters(
                Arrays.asList(new TargetInfo[] { ti }).stream()
                        .map(e -> e.getPositionPacket()), qh);

        return StreamingUtil.createStreamingOutput(packets,
                AisPacketOutputSinks.JSON_MESSAGE);
    }

    @GET
    @Path("/dynamic/")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput getPositions(@Context UriInfo info) {
        QueryParameterHelper qh = new QueryParameterHelper(info);
        Predicate<AisPacketSource> predSource = qh.getSourcePredicate();
        predSource = (predSource == null) ? e -> true : predSource;

        Predicate<TargetInfo> predTarget = qh.getTargetPredicate();
        predTarget = predTarget.and(TargetInfoFilters.filterOnHasPosition());
        predTarget = (qh.getArea() != null) ? qh.getTargetAreaFilter() : predTarget;

        TargetTracker tt = get(TargetTracker.class);

        Stream<TargetInfo> s = tt.findTargets8(predSource, predTarget);
        Stream<AisPacket> sPacket = s.map(e -> e.getPositionPacket()).filter(
                o -> o != null);

        Iterable<AisPacket> packets = applyFilters(sPacket, qh);

        return StreamingUtil.createStreamingOutput(packets,
                AisPacketOutputSinks.JSON_POS_LIST);
    }

    /**
     * Helper function for applying filters from Stream to Iterable.
     * @param streamPackets
     * @param qh
     * @return
     */
    private Iterable<AisPacket> applyFilters(Stream<AisPacket> streamPackets,
            QueryParameterHelper qh) {
        // reapplying filters on packet stream
        Iterable<AisPacket> packets = (Iterable<AisPacket>) streamPackets::iterator;

        // filters
        packets = qh.applyPacketFilter(packets);

        return packets;
    }

    @GET
    @Path("/packets/json")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput getPacketsJson(@Context UriInfo info) {
        Stream<AisPacket> packets = getPacketStream(info);
        return StreamingUtil.createStreamingOutput(
                (Iterable<AisPacket>) packets::iterator,
                AisPacketOutputSinks.JSON_MESSAGE);
    }

    @GET
    @Path("/packets/")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput getPackets(@Context UriInfo info) {
        Stream<AisPacket> packets = getPacketStream(info);
        return StreamingUtil.createStreamingOutput(
                (Iterable<AisPacket>) packets::iterator,
                AisPacketOutputSinks.OUTPUT_TO_TEXT);
    }

    private Stream<AisPacket> getPacketStream(UriInfo info) {
        QueryParameterHelper qh = new QueryParameterHelper(info);

        Predicate<AisPacketSource> predSource = qh.getSourcePredicate();
        predSource = (predSource == null) ? e -> true : predSource;

        Predicate<TargetInfo> predTarget = qh.getTargetPredicate();
        predTarget = (predTarget == null) ? e -> true : predTarget;
        
        predTarget = (qh.getArea() != null) ? qh.getTargetAreaFilter() : predTarget;

        TargetTracker tt = get(TargetTracker.class);

        Stream<TargetInfo> s = tt.findTargets8(predSource, predTarget);
        Stream<AisPacket[]> sPackets = s.map(e -> e.getPackets()).filter(
                o -> o != null);

        final ConcurrentLinkedDeque<AisPacket> packets = new ConcurrentLinkedDeque<AisPacket>();
        sPackets.sequential().forEach(e -> {
            for (int i = 0; i < e.length; i++) {
                packets.add(e[i]);
            }
        });

        return packets.stream();
    }

}

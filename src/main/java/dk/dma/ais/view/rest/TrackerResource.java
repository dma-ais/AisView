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
import java.util.concurrent.ConcurrentSkipListSet;
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
import dk.dma.ais.packet.AisPacketOutputSinks;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.tracker.TargetInfo;
import dk.dma.ais.tracker.TargetTracker;
import dk.dma.ais.view.common.util.TargetInfoFilters;
import dk.dma.commons.util.JSONObject;
import dk.dma.commons.web.rest.StreamingUtil;

/**
 * 
 * @author Jens Tuxen
 */
@Path("/tracker")
public class TrackerResource extends AbstractTrackerResource {

    /**
     * @param handler
     */
    public TrackerResource() {
        super();
    }
    
    /**
     * Returns a list of source IDs currently available in the tracker
     * @return a list of source IDs in the source
     */
    @GET
    @Path("/source/ids")
    public JSONObject getSourceIDs() {
        
        TargetTracker tt = TrackerResource.this.get(TargetTracker.class);
        
        ConcurrentSkipListSet<String> ids = new ConcurrentSkipListSet<String>();
        
        tt.findTargets(source-> {
            if (source.getSourceId() != null) {
                ids.add(source.getSourceId());
            }
            return false;
        },target->true);
        
        return JSONObject.singleList("sourceIDs", ids.toArray());
    }
    
    /**
     * Returns a list of source regions
     * @return a list of source region numbers
     */
    @GET
    @Path("/source/regions")
    public JSONObject getSourceRegions() {
        
        TargetTracker tt = TrackerResource.this.get(TargetTracker.class);
        
        ConcurrentSkipListSet<String> ids = new ConcurrentSkipListSet<String>();
        
        tt.findTargets(source-> {
            if (source.getSourceRegion() != null) {
                ids.add(source.getSourceRegion());
            }
            return false;
        },target->true);
        
        
        return JSONObject.singleList("sourceregions", ids.toArray());
    }
    
    
    @GET
    @Path("/count/targetinfo")
    @Produces(MediaType.TEXT_PLAIN)
    public int getTargetInfoCount(@Context UriInfo info) {
        QueryParameterHelper qh = new QueryParameterHelper(info);
        Predicate<AisPacketSource> predSource = (qh.getSourcePredicate() == null) ? e->true : qh.getSourcePredicate();
        Predicate<TargetInfo> predTarget = qh.getTargetPredicate();
        predTarget = (predTarget == null) ? e -> true : predTarget;        
        predTarget = (qh.getArea() != null) ? qh.getTargetAreaFilter() : predTarget;
        return get(TargetTracker.class).findTargets(predSource, predTarget).size();
    }    

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public int getCount(@Context UriInfo info) {
        QueryParameterHelper qh = new QueryParameterHelper(info);
        Predicate<AisPacketSource> predSource = (qh.getSourcePredicate() == null) ? e->true : qh.getSourcePredicate();
        Predicate<TargetInfo> predTarget = qh.getTargetPredicate();
        predTarget = (predTarget == null) ? e -> true : predTarget;        
        predTarget = (qh.getArea() != null) ? qh.getTargetAreaFilter() : predTarget;
        return get(TargetTracker.class).countNumberOfTargets(predSource,predTarget);
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
                qh.getOutputSink());
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

        Stream<AisPacket[]> sPackets = s.map(e -> e.getStaticPackets());

        final ConcurrentLinkedDeque<AisPacket> packets = new ConcurrentLinkedDeque<AisPacket>();
        sPackets.forEach(e -> {
            for (int i = 0; i < e.length; i++) {
                packets.add(e[i]);
            }
        });

        Iterable<AisPacket> p = applyFilters(packets.stream(), qh);

        return StreamingUtil.createStreamingOutput(p,
                qh.getOutputSink());
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
                qh.getOutputSink());
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
                qh.getOutputSink());
    }

    @GET
    @Path("/packets/json")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput getPacketsJson(@Context UriInfo info) {
        QueryParameterHelper qh = new QueryParameterHelper(info);
        Stream<AisPacket> packets = getPacketStream(info, qh);
        return StreamingUtil.createStreamingOutput(
                (Iterable<AisPacket>) packets::iterator,
                AisPacketOutputSinks.jsonMessageSink());
    }

    @GET
    @Path("/packets/")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput getPackets(@Context UriInfo info) {
        QueryParameterHelper qh = new QueryParameterHelper(info);
        Stream<AisPacket> packets = getPacketStream(info, qh);
        return StreamingUtil.createStreamingOutput(
                (Iterable<AisPacket>) packets::iterator,qh.getOutputSink());
    }

}

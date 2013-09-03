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

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.packet.AisPacketOutputSinks;
import dk.dma.ais.store.AisStoreConnection;
import dk.dma.ais.store.AisStoreQueryBuilder;
import dk.dma.ais.store.AisStoreQueryResult;
import dk.dma.ais.view.JobManager;
import dk.dma.ais.view.JobManager.Job;
import dk.dma.commons.util.Iterables;
import dk.dma.commons.util.JSONObject;
import dk.dma.commons.web.rest.AbstractResource;
import dk.dma.commons.web.rest.StreamingUtil;
import dk.dma.commons.web.rest.query.QueryParameterValidators;

/**
 * Resources that query AisStore.
 * 
 * @author Kasper Nielsen
 */
@Path("/store")
public class AisStoreResource extends AbstractResource {

    /**
     * Returns a list of source IDs in the source. This one is hard coded for now
     * 
     * @return a list of source IDs in the source
     */
    @GET
    @Path("/sourceIDs")
    public JSONObject getSourceIDs() {
        return JSONObject.singleList("sourceIDs", "AISD", "IALA", "AISSAT", "MSSIS", "AISHUB");
    }

    /**
     * Used to query job status for long running AisStore access jobs.
     */
    @GET
    @Produces("application/json")
    @Path("/status/{jobId : \\w+}")
    public JSONObject queryStatus(@Context UriInfo info, @PathParam("jobId") String jobId) {
        if (jobId != null) {
            Job j = get(JobManager.class).getResult(jobId);
            if (j != null) {
                return j.toJSON();
            }
        }
        return new JSONObject();
    }

    @GET
    @Produces("text/plain")
    @Path("/query")
    public StreamingOutput query(@Context UriInfo info) {
        QueryHelper p = new QueryHelper(info);

        // Create builder, we first need to determine which of the 3 AisStore tables we need to use
        AisStoreQueryBuilder b;
        if (p.getMMSIs().length > 0) {
            b = AisStoreQueryBuilder.forMmsi(p.getMMSIs());
        } else if (p.getArea() != null) {
            b = AisStoreQueryBuilder.forArea(p.getArea());
        } else {
            b = AisStoreQueryBuilder.forTime();
        }
        // Set various properties for the query builder
        b.setFetchSize(QueryParameterValidators.getParameterAsInt(info, "fetchSize", 3000));
        b.setInterval(p.getInterval());

        // Create the query
        AtomicLong counter = new AtomicLong();
        AisStoreQueryResult query = get(AisStoreConnection.class).execute(b);
        Iterable<AisPacket> q = query;
        // Apply filters from the user
        // Apply area filter again, problem with position tagging of static data
        q = p.applySourceFilter(q);
        q = p.applyLimitFilter(q); // WARNING: Must be the last filter (if other filters reject packets)
        q = Iterables.counting(q, counter);
        if (p.jobId != null) {
            get(JobManager.class).addJob(p.jobId, query, counter);
        }
        return StreamingUtil.createStreamingOutput(q, p.getOutputSink(), query);
    }

    @GET
    @Path("/track/{mmsi : \\d+}")
    @Produces("application/json")
    public StreamingOutput pastTrack(@Context UriInfo info, @PathParam("mmsi") int mmsi) {
        QueryHelper p = new QueryHelper(info);

        // Execute the query
        AisStoreQueryBuilder b = AisStoreQueryBuilder.forMmsi(mmsi);
        b.setInterval(p.getInterval());

        // Create the query
        Iterable<AisPacket> query = get(AisStoreConnection.class).execute(b);

        // Apply filters from the user
        query = Iterables.filter(query, AisPacketFilters.filterOnMessageType(IVesselPositionMessage.class));
        query = p.applySourceFilter(query);
        query = p.applyPositionSampler(query); // WARNING: Must be the second last filter
        query = p.applyLimitFilter(query); // WARNING: Must be the last filter (if other filters reject packets)
        return StreamingUtil.createStreamingOutput(query, AisPacketOutputSinks.PAST_TRACK_JSON);
    }

}

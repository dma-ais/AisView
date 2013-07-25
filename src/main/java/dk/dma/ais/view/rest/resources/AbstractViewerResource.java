/*
 * Copyright (c) 2008 Kasper Nielsen.
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
package dk.dma.ais.view.rest.resources;

import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.joda.time.Interval;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.packet.AisPackets;
import dk.dma.ais.store.AisStore;
import dk.dma.ais.store.AisStores;
import dk.dma.ais.view.AisViewer;
import dk.dma.ais.view.target.TargetTracker;
import dk.dma.commons.util.io.OutputStreamSink;
import dk.dma.enav.util.function.Predicate;

/**
 * 
 * @author Kasper Nielsen
 */
public class AbstractViewerResource {

    public static final String VIEWER_ATTRIBUTE = "viewer";

    @Context
    ServletConfig servletConfig;

    public Predicate<? super AisPacket> getFilter(UriInfo info) {
        List<String> filters = info.getQueryParameters().get("filter");
        if (filters == null || filters.isEmpty()) {
            return Predicate.TRUE;
        }
        Predicate<AisPacket> p = AisPacketFilters.parseSourceFilter(filters.get(0));
        for (int i = 1; i < filters.size(); i++) {
            p = p.and(AisPacketFilters.parseSourceFilter(filters.get(i)));
        }
        return p;
    }

    public OutputStreamSink<AisPacket> getOutputSink(UriInfo info) {
        String output = getOneOr(info, "output", "raw").toLowerCase();
        switch (output) {
        case "raw":
            return AisPackets.OUTPUT_TO_TEXT;
        case "table":
            String columns = getOne(info, "columns",
                    "A query parameter (columns), must be present when using table output");
            return AisPackets.newTableSink(columns, !info.getQueryParameters().containsKey("noHeader"),
                    getOneOr(info, "separator", ";"));
        }

        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("Unknown output format [output=" + output + "]\n").type(MediaType.TEXT_PLAIN).build());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Predicate<AisPacketSource> getSourceFilter(UriInfo info) {
        List<String> filters = info.getQueryParameters().get("sourceFilter");
        if (filters == null || filters.isEmpty()) {
            return (Predicate) Predicate.TRUE;
        }
        Predicate<AisPacketSource> p = AisPacketSource.createPredicate(filters.get(0));
        for (int i = 1; i < filters.size(); i++) {
            p = p.and(AisPacketSource.createPredicate(filters.get(i)));
        }
        return p;
    }

    final AisStore getStore() {
        return getViewer().getAisStore();
    }

    final TargetTracker getTracker() {
        return getViewer().getTracker();
    }

    final AisViewer getViewer() {
        return (AisViewer) servletConfig.getServletContext().getAttribute(VIEWER_ATTRIBUTE);
    }

    static Interval findInterval(UriInfo info) {
        List<String> intervals = info.getQueryParameters().get("interval");
        if (intervals == null || intervals.size() == 0) {
            return new Interval(0, Long.MAX_VALUE);
        } else if (intervals.size() > 1) {
            throw new IllegalArgumentException("Multiple interval parameters defined: " + intervals);
        }
        return AisStores.toInterval(intervals.get(0));
    }

    static String getOne(UriInfo info, String param, String errorMsg) {
        String s = getOneOr(info, param, null);
        if (s == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(errorMsg + "\n")
                    .type(MediaType.TEXT_PLAIN).build());
        }
        return s;
    }

    static String getOneOr(UriInfo info, String param, String or) {
        List<String> params = info.getQueryParameters().get(param);
        if (params == null || params.isEmpty()) {
            return or;
        } else if (params.size() > 1) {
            throw new WebApplicationException("Only 1 query parameter of " + param + " allowed");
        }
        return params.get(0);
    }
}

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
package dk.dma.ais.view.rest.resources.util;

import static dk.dma.commons.web.rest.UriQueryUtil.getOne;
import static dk.dma.commons.web.rest.UriQueryUtil.getOneOrZeroParametersOrFail;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.joda.time.Interval;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.packet.AisPacketOutputStreamSinks;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.commons.util.DateTimeUtil;
import dk.dma.commons.util.io.OutputStreamSink;
import dk.dma.commons.web.rest.UriQueryUtil;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.util.function.Predicate;

/**
 * 
 * @author Kasper Nielsen
 */
public class ParameterExtractor {

    public static Area findArea(UriInfo info) {
        String box = UriQueryUtil.getOneOrZeroParametersOrFail(info, "box", null);
        if (box != null) {
            String[] str = box.split(",");
            if (str.length != 4) {
                throw new UnsupportedOperationException("A box must contain exactly 4 points, was " + str.length + "("
                        + box + ")");
            }
            double lat1 = Double.parseDouble(str[0]);
            double lon1 = Double.parseDouble(str[1]);
            double lat2 = Double.parseDouble(str[2]);
            double lon2 = Double.parseDouble(str[3]);
            Position p1 = Position.create(lat1, lon1);
            Position p2 = Position.create(lat2, lon2);
            return BoundingBox.create(p1, p2, CoordinateSystem.CARTESIAN);
        }
        return null;
    }

    public static Interval findInterval(UriInfo info) {
        List<String> intervals = info.getQueryParameters().get("interval");
        if (intervals == null || intervals.size() == 0) {
            return new Interval(0, Long.MAX_VALUE);
        } else if (intervals.size() > 1) {
            throw new IllegalArgumentException("Multiple interval parameters defined: " + intervals);
        }
        return DateTimeUtil.toInterval(intervals.get(0));
    }

    public static OutputStreamSink<AisPacket> getOutputSink(UriInfo info) {
        String output = getOneOrZeroParametersOrFail(info, "output", "raw").toLowerCase();
        switch (output) {
        case "raw":
            return AisPacketOutputStreamSinks.OUTPUT_TO_TEXT;
        case "table":
            String columns = getOne(info, "columns",
                    "A query parameter (columns), must be present when using table output");
            return AisPacketOutputStreamSinks.newTableSink(columns, !info.getQueryParameters().containsKey("noHeader"),
                    getOneOrZeroParametersOrFail(info, "separator", ";"));
        }

        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("Unknown output format [output=" + output + "]\n").type(MediaType.TEXT_PLAIN).build());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Predicate<AisPacketSource> getPacketSourceFilter(UriInfo info) {
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Predicate<AisPacket> getSourceFilter(UriInfo info) {
        List<String> filters = info.getQueryParameters().get("filter");
        if (filters == null || filters.isEmpty()) {
            return (Predicate) Predicate.TRUE;
        }
        Predicate<AisPacket> p = AisPacketFilters.parseSourceFilter(filters.get(0));
        for (int i = 1; i < filters.size(); i++) {
            p = p.and(AisPacketFilters.parseSourceFilter(filters.get(i)));
        }
        return p;
    }

}

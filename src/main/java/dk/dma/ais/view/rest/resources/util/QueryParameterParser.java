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
package dk.dma.ais.view.rest.resources.util;

import static dk.dma.commons.web.rest.UriQueryUtil.getOne;
import static dk.dma.commons.web.rest.UriQueryUtil.getOneOrZeroParametersOrFail;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.joda.time.Interval;
import org.joda.time.Period;

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
 * This class is responsible for parsing query parameters.
 * 
 * @author Kasper Nielsen
 */
class QueryParameterParser {

    public static Integer findMinimumDistanceMeters(UriInfo info) {
        return UriQueryUtil.getOneOrZeroIntParametersOrFail(info, "minDistance", null);
    }

    public static Long findMinimumDurationMS(UriInfo info) {
        String dur = UriQueryUtil.getOneOrZeroParametersOrFail(info, "minDuration", null);
        return dur == null ? null : Period.parse(dur).toStandardSeconds().getSeconds() * 1000L;
    }

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
        String interval = UriQueryUtil.getOneOrZeroParametersOrFail(info, "interval", null);
        if (interval == null) {
            throw new IllegalArgumentException("Must define at least one interval");
        }
        return DateTimeUtil.toInterval(interval);
    }

    static OutputStreamSink<AisPacket> getOutputSink(UriInfo info) {
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

    public static Predicate<AisPacketSource> getPacketSourceFilter(UriInfo info) {
        List<String> filters = UriQueryUtil.getParameters(info, "filter");
        if (filters.isEmpty()) {
            return null;
        }
        Predicate<AisPacketSource> p = AisPacketSource.createPredicate(filters.get(0));
        for (int i = 1; i < filters.size(); i++) {
            p = p.and(AisPacketSource.createPredicate(filters.get(i)));
        }
        return p;
    }

    public static Predicate<AisPacket> getSourceFilter(UriInfo info) {
        List<String> filters = UriQueryUtil.getParameters(info, "filter");
        if (filters.isEmpty()) {
            return null;
        }
        Predicate<AisPacket> p = AisPacketFilters.parseSourceFilter(filters.get(0));
        for (int i = 1; i < filters.size(); i++) {
            p = p.and(AisPacketFilters.parseSourceFilter(filters.get(i)));
        }
        return p;
    }

}

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
package dk.dma.ais.view.rest.resources;

import static dk.dma.commons.web.rest.query.QueryParameterValidators.getParameter;
import static dk.dma.commons.web.rest.query.QueryParameterValidators.getParameterAsIntWithRange;
import static dk.dma.commons.web.rest.query.QueryParameterValidators.getParameterWithCustomErrorMessage;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.joda.time.Interval;
import org.joda.time.Period;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.packet.AisPacketOutputSinks;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.packet.AisPacketStream;
import dk.dma.commons.util.DateTimeUtil;
import dk.dma.commons.util.Iterables;
import dk.dma.commons.util.io.OutputStreamSink;
import dk.dma.commons.web.rest.query.QueryParameterValidators;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.util.function.Predicate;

/**
 * A small helper class to extract query information.
 * 
 * @author Kasper Nielsen
 */
class QueryHelper {

    final Area area;

    final Interval interval;
    final Integer limit;

    final Integer minDistance;

    final Long minDuration;

    final int[] mmsis;
    final OutputStreamSink<AisPacket> outputSink;

    final Predicate<? super AisPacket> sourceFilter;
    final UriInfo uriInfo;

    public QueryHelper(UriInfo uriInfo) {
        this.uriInfo = requireNonNull(uriInfo);
        this.area = findArea(uriInfo);
        this.interval = findInterval(uriInfo);
        String limit = getParameter(uriInfo, "limit", null);
        this.limit = limit == null ? null : Integer.parseInt(limit);
        Set<Integer> mmsi = new HashSet<>(QueryParameterValidators.getParametersAsInt(uriInfo, "mmsi"));
        this.mmsis = Ints.toArray(mmsi);
        sourceFilter = getSourceFilter(uriInfo);

        minDistance = getParameterAsIntWithRange(uriInfo, "minDistance", Range.atLeast(0));
        minDuration = findMinimumDurationMS(uriInfo);
        outputSink = getOutputSink(uriInfo);
    }

    public Iterable<AisPacket> applyAreaFilter(Iterable<AisPacket> i) {
        return area == null ? i : Iterables.filter(i, AisPacketFilters.filterOnMessagePositionWithin(area));
    }

    public AisPacketStream applyLimitFilter(AisPacketStream s) {
        return limit == null ? s : s.limit(limit);
    }

    public Iterable<AisPacket> applyLimitFilter(Iterable<AisPacket> i) {
        return limit == null ? i : com.google.common.collect.Iterables.limit(i, limit);
    }

    public Iterable<AisPacket> applyPositionSampler(Iterable<AisPacket> i) {
        if (minDistance == null && minDuration == null) {
            return i;
        }
        return Iterables.filter(i, AisPacketFilters.samplingFilter(minDistance, minDuration));
    }

    public AisPacketStream applySourceFilter(AisPacketStream s) {
        return sourceFilter == null ? s : s.filter(sourceFilter);
    }

    public Iterable<AisPacket> applySourceFilter(Iterable<AisPacket> i) {
        return sourceFilter == null ? i : Iterables.filter(i, sourceFilter);
    }

    public Predicate<AisPacketSource> getSourcePredicate() {
        return getPacketSourceFilter(uriInfo);
    }

    public Area getArea() {
        return area;
    }

    /**
     * @return the interval
     */
    public Interval getInterval() {
        return interval;
    }

    public int[] getMMSIs() {
        return Arrays.copyOf(mmsis, mmsis.length);
    }

    /**
     * @return the outputSink
     */
    public OutputStreamSink<AisPacket> getOutputSink() {
        return outputSink;
    }

    private static Long findMinimumDurationMS(UriInfo info) {
        String dur = QueryParameterValidators.getParameter(info, "minDuration", null);
        return dur == null ? null : Period.parse(dur).toStandardSeconds().getSeconds() * 1000L;
    }

    private static Area findArea(UriInfo info) {
        String box = QueryParameterValidators.getParameter(info, "box", null);
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

    private static Interval findInterval(UriInfo info) {
        String interval = QueryParameterValidators.getParameter(info, "interval", null);
        if (interval == null) {
            throw new IllegalArgumentException("Must define at least one interval");
        }
        return DateTimeUtil.toInterval(interval);
    }

    private static OutputStreamSink<AisPacket> getOutputSink(UriInfo info) {
        String output = getParameter(info, "output", "raw").toLowerCase();
        switch (output) {
        case "raw":
            return AisPacketOutputSinks.OUTPUT_TO_TEXT;
        case "table":
            String columns = getParameterWithCustomErrorMessage(info, "columns",
                    "A query parameter (columns), must be present when using table output");
            return AisPacketOutputSinks.newTableSink(columns, !info.getQueryParameters().containsKey("noHeader"),
                    getParameter(info, "separator", ";"));
        }

        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("Unknown output format [output=" + output + "]\n").type(MediaType.TEXT_PLAIN).build());
    }

    private static Predicate<AisPacketSource> getPacketSourceFilter(UriInfo info) {
        List<String> filters = QueryParameterValidators.getParameters(info, "filter");
        if (filters.isEmpty()) {
            return null;
        }
        Predicate<AisPacketSource> p = AisPacketSource.createPredicate(filters.get(0));
        for (int i = 1; i < filters.size(); i++) {
            p = p.and(AisPacketSource.createPredicate(filters.get(i)));
        }
        return p;
    }

    private static Predicate<AisPacket> getSourceFilter(UriInfo info) {
        List<String> filters = QueryParameterValidators.getParameters(info, "filter");
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

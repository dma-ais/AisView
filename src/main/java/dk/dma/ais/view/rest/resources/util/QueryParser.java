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

import static dk.dma.ais.view.rest.resources.util.QueryParameterParser.getSourceFilter;
import static dk.dma.commons.web.rest.UriQueryUtil.getOneOrZeroParametersOrFail;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

import org.joda.time.Interval;

import com.google.common.primitives.Ints;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.packet.AisPacketStream;
import dk.dma.commons.util.Iterables;
import dk.dma.commons.util.io.OutputStreamSink;
import dk.dma.commons.web.rest.UriQueryUtil;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.util.function.Predicate;

/**
 * 
 * @author Kasper Nielsen
 */
public class QueryParser {

    final Area area;

    final Interval interval;
    final Integer limit;

    final Integer minDistance;

    final Long minDuration;

    final int[] mmsis;
    final OutputStreamSink<AisPacket> outputSink;

    final Predicate<? super AisPacket> sourceFilter;
    final UriInfo uriInfo;

    public QueryParser(UriInfo uriInfo) {
        this.uriInfo = requireNonNull(uriInfo);
        this.area = QueryParameterParser.findArea(uriInfo);
        this.interval = QueryParameterParser.findInterval(uriInfo);
        String limit = getOneOrZeroParametersOrFail(uriInfo, "limit", null);
        this.limit = limit == null ? null : Integer.parseInt(limit);
        Set<Integer> mmsi = new HashSet<>(UriQueryUtil.getParametersAsInt(uriInfo, "mmsi"));
        this.mmsis = Ints.toArray(mmsi);
        sourceFilter = getSourceFilter(uriInfo);

        minDistance = QueryParameterParser.findMinimumDistanceMeters(uriInfo);
        minDuration = QueryParameterParser.findMinimumDurationMS(uriInfo);
        outputSink = QueryParameterParser.getOutputSink(uriInfo);
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
        return Iterables.filter(i, new SamplerPredicate(minDistance, minDuration));
    }

    public AisPacketStream applySourceFilter(AisPacketStream s) {
        return sourceFilter == null ? s : s.filter(sourceFilter);
    }

    public Iterable<AisPacket> applySourceFilter(Iterable<AisPacket> i) {
        return sourceFilter == null ? i : Iterables.filter(i, sourceFilter);
    }

    public Predicate<AisPacketSource> getSourcePredicate() {
        return QueryParameterParser.getPacketSourceFilter(uriInfo);
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
}

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

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.packet.AisPacketFiltersStateful;
import dk.dma.ais.packet.AisPacketOutputSinkJsonObject;
import dk.dma.ais.packet.AisPacketOutputSinks;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.packet.AisPacketStream;
import dk.dma.ais.tracker.targetTracker.TargetInfo;
import dk.dma.ais.view.common.util.TargetInfoFilters;
import dk.dma.commons.util.DateTimeUtil;
import dk.dma.commons.util.Iterables;
import dk.dma.commons.util.io.OutputStreamSink;
import dk.dma.commons.web.rest.query.QueryParameterValidators;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static dk.dma.commons.web.rest.query.QueryParameterValidators.getParameter;
import static dk.dma.commons.web.rest.query.QueryParameterValidators.getParameterAsInt;
import static dk.dma.commons.web.rest.query.QueryParameterValidators.getParameterAsIntWithRange;
import static dk.dma.commons.web.rest.query.QueryParameterValidators.getParameterWithCustomErrorMessage;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * A small helper class to extract query information.
 * 
 * @author Kasper Nielsen
 */
class QueryParameterHelper {

    private static final Logger LOG = LoggerFactory
            .getLogger(QueryParameterHelper.class);

    /** An optional area for the query. */
    final BoundingBox area;

    /** The interval for the query */
    final Interval interval;

    /** The time for which to generate snapshot in KML queries */
    final DateTime kmlSnapshotAt;

    final Integer limit;

    final Integer minDistance;

    final Long minDuration;

    final Long duplicateWindow;

    final Integer primaryMmsi;

    final Integer secondaryMmsi;

    final String title;

    final String description;

    final Integer interpolationStepSecs;

    final long timeToRun = -1;

    final int[] mmsis;

    final OutputStreamSink<AisPacket> outputSink;

    /** A predicate on the source of each packet. */
    final Predicate<? super AisPacket> sourceFilter;

    /** A general AisPacket filter on source, target and message */
    final Predicate<? super AisPacket> packetFilter;

    final UriInfo uriInfo;

    final String jobId;

    final boolean createSituationFolder;

    final boolean createMovementsFolder;

    final boolean createTracksFolder;

    public QueryParameterHelper(UriInfo uriInfo) {
        this.uriInfo = requireNonNull(uriInfo);
        this.area = findBoundingBox(uriInfo);
        this.interval = findInterval(uriInfo);

        this.createSituationFolder = findCreateSituationFolder(uriInfo);
        this.createMovementsFolder = findCreateMovementsFolder(uriInfo);
        this.createTracksFolder = findCreateTracksFolder(uriInfo);

        this.kmlSnapshotAt = findAt(uriInfo);
        String limit = getParameter(uriInfo, "limit", null);
        this.limit = limit == null ? null : Integer.parseInt(limit);
        Set<Integer> mmsi = new HashSet<>(
                QueryParameterValidators.getParametersAsInt(uriInfo, "mmsi"));
        this.mmsis = Ints.toArray(mmsi);
        sourceFilter = getSourceFilter(uriInfo);
        packetFilter = getPacketFilter(uriInfo);

        minDistance = getParameterAsIntWithRange(uriInfo, "minDistance", null,
                Range.atLeast(0));
        minDuration = findMinimumDurationMS(uriInfo);
        String duplicateWindow = getParameter(uriInfo, "duplicateWindow", null);
        this.duplicateWindow = (duplicateWindow == null) ? null : Long.parseLong(duplicateWindow) * 1000; // to ms
        primaryMmsi = findPrimaryMmsi(uriInfo);
        secondaryMmsi = findSecondaryMmsi(uriInfo);
        title = getParameter(uriInfo, "title", null);
        description = getParameter(uriInfo, "description", null);
        interpolationStepSecs = getParameterAsInt(uriInfo, "interpolation",
                null);

        outputSink = getOutputSink(uriInfo);
        jobId = QueryParameterValidators.getParameter(uriInfo, "jobId", null);

        LOG.debug(toString());
    }

    public Iterable<AisPacket> applyAreaFilter(Iterable<AisPacket> i) {
        return area == null ? i : Iterables.filter(i,
                AisPacketFilters.filterOnMessagePositionWithin(area));
    }

    public Iterable<AisPacket> applyTargetFilterArea(Iterable<AisPacket> i,
            AisPacketFiltersStateful state) {
        return area == null ? i : Iterables.filter(i,
                state.filterOnTargetPositionWithin(area));
    }

    public AisPacketStream applyLimitFilter(AisPacketStream s) {
        return limit == null ? s : s.limit(limit);
    }

    public Iterable<AisPacket> applyLimitFilter(Iterable<AisPacket> i) {
        return limit == null ? i : com.google.common.collect.Iterables.limit(i,
                limit);
    }

    public Iterable<AisPacket> applyPositionSampler(Iterable<AisPacket> i) {
        if (minDistance == null && minDuration == null) {
            return i;
        }
        return Iterables.filter(i,
                AisPacketFilters.samplingFilter(minDistance, minDuration));
    }

    public Iterable<AisPacket> applyDuplicateFilter(Iterable<AisPacket> i) {
        if (duplicateWindow == null) {
            return i;
        }
        return Iterables.filter(i,
                AisPacketFilters.duplicateFilter(duplicateWindow));
    }

    public Iterable<AisPacket> applyTargetPositionSampler(Iterable<AisPacket> i) {
        if (minDistance == null && minDuration == null) {
            return i;
        }
        return Iterables.filter(i,
                AisPacketFilters.targetSamplingFilter(minDistance, minDuration));
    }

    public AisPacketStream applySourceFilter(AisPacketStream s) {
        return sourceFilter == null ? s : s.filter(sourceFilter);
    }

    public Iterable<AisPacket> applySourceFilter(Iterable<AisPacket> i) {
        return sourceFilter == null ? i : Iterables.filter(i, sourceFilter);
    }

    public Iterable<AisPacket> applyPacketFilter(Iterable<AisPacket> i) {
        return packetFilter == null ? i : Iterables.filter(i, packetFilter);
    }

    public BiPredicate<AisPacketSource, TargetInfo> getSourceAndTargetPredicate() {
        final Predicate<AisPacketSource> ps = getSourcePredicate();
        final Predicate<TargetInfo> pt = getTargetPredicate();
        return new BiPredicate<AisPacketSource, TargetInfo>() {
            @Override
            public boolean test(AisPacketSource t, TargetInfo u) {
                return ps.test(t) && pt.test(u);
            }
        };
    }

    public Predicate<TargetInfo> getTargetPredicate() {
        return e -> true;
    }

    public Predicate<TargetInfo> getTargetAreaFilter() {
        return area != null ? TargetInfoFilters.filterOnBoundingBox(area)
                : null;
    }

    public Predicate<AisPacketSource> getSourcePredicate() {
        return getPacketSourceFilter(uriInfo);
    }

    public Predicate<AisPacket> getPacketFilter() {
        return getPacketFilter(uriInfo);
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
        String dur = QueryParameterValidators.getParameter(info, "minDuration",
                null);
        return dur == null ? null : Period.parse(dur).toStandardSeconds()
                .getSeconds() * 1000L;
    }

    private static Integer findPrimaryMmsi(UriInfo info) {
        String primaryMmsi = QueryParameterValidators.getParameter(info,
                "primaryMmsi", null);
        return primaryMmsi == null ? null : Integer.parseInt(primaryMmsi);
    }

    private static Integer findSecondaryMmsi(UriInfo info) {
        String secondaryMmsi = QueryParameterValidators.getParameter(info,
                "secondaryMmsi", null);
        return secondaryMmsi == null ? null : Integer.parseInt(secondaryMmsi);
    }

    private static BoundingBox findBoundingBox(UriInfo info) {
        String box = QueryParameterValidators.getParameter(info, "box", null);
        if (box != null) {
            String[] str = box.split(",");
            if (str.length != 4) {
                throw new UnsupportedOperationException(
                        "A box must contain exactly 4 points, was "
                                + str.length + "(" + box + ")");
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
        String interval = QueryParameterValidators.getParameter(info,
                "interval", null);
        if (interval == null) {
            return null;
            // throw new
            // IllegalArgumentException("Must define kmlSnapshotAt least one interval");
        }
        return DateTimeUtil.toInterval(interval);
    }

    private static boolean findCreateSituationFolder(UriInfo info) {
        return QueryParameterValidators.getParameter(info,
                "situationFolderEnabled", null) != null;
    }

    private static boolean findCreateMovementsFolder(UriInfo info) {
        return QueryParameterValidators.getParameter(info,
                "movementsFolderEnabled", null) != null;
    }

    private static boolean findCreateTracksFolder(UriInfo info) {
        return QueryParameterValidators.getParameter(info,
                "tracksFolderEnabled", null) != null;
    }

    private static DateTime findAt(UriInfo info) {
        String at = QueryParameterValidators.getParameter(info, "at", null);
        return isBlank(at) ? null : DateTime.parse(at);
    }

    @SuppressWarnings("unchecked")
    private static OutputStreamSink<AisPacket> getOutputSink(UriInfo info) {
        String output = getParameter(info, "output", "raw").toLowerCase();
        switch (output) {
        case "raw":
            return AisPacketOutputSinks.OUTPUT_TO_TEXT;
        case "table":
            String columns = getParameterWithCustomErrorMessage(
                    info,
                    "columns",
                    "A query parameter (columns), must be present when using table output. Example: columns=time;mmsi;lat;lon");
            return AisPacketOutputSinks.newTableSink(columns, !info
                    .getQueryParameters().containsKey("noHeader"),
                    getParameter(info, "separator", ";"));
        case "prefixed_sentences":
            return AisPacketOutputSinks.OUTPUT_PREFIXED_SENTENCES;
        case "output_to_kml":
            return AisPacketOutputSinks.OUTPUT_TO_KML();
        case "kml":
            return AisPacketOutputSinks.OUTPUT_TO_KML();

        case "jsonobject":
            String format = getParameter(info, "columns", "");
            if (format.equals("")) {
                format = AisPacketOutputSinkJsonObject.ALLCOLUMNS;
            }
            return AisPacketOutputSinks.jsonObjectSink(format);
        case "json":
            return AisPacketOutputSinks.jsonMessageSink();
            
            
        // this will work for static fields like OUTPUT_TO_HTML,
        // OUTPUT_TO_TEXT
        default:
            try {
                return (OutputStreamSink<AisPacket>) AisPacketOutputSinks.class
                        .getField(output.toUpperCase()).get(null);
            } catch (IllegalArgumentException | IllegalAccessException
                    | NoSuchFieldException | SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        throw new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST)
                .entity("Unknown output format [output=" + output + "]\n")
                .type(MediaType.TEXT_PLAIN).build());
    }

    private static Predicate<AisPacketSource> getPacketSourceFilter(UriInfo info) {
        List<String> filters = QueryParameterValidators.getParameters(info,
                "filter");
        if (filters.isEmpty()) {
            return null;
        }

        Predicate<AisPacketSource> p = null;
        try {
            p = AisPacketSource.createPredicate(filters.get(0));
            for (int i = 1; i < filters.size(); i++) {
                p = p.and(AisPacketSource.createPredicate(filters.get(i)));
            }
        } catch (NullPointerException e) {

        }

        return p;
    }

    private static Predicate<AisPacket> getSourceFilter(UriInfo info) {
        List<String> filters = QueryParameterValidators.getParameters(info,
                "filter");

        if (filters.isEmpty()) {
            return null;
        }
        Predicate<AisPacket> p = AisPacketFilters.parseSourceFilter(filters
                .get(0));
        for (int i = 1; i < filters.size(); i++) {
            p = p.and(AisPacketFilters.parseSourceFilter(filters.get(i)));
        }
        return p;
    }

    /**
     */
    private static Predicate<AisPacket> getPacketFilter(UriInfo info) {
        List<String> filters = QueryParameterValidators.getParameters(info,
                "filter");
        if (filters.isEmpty()) {
            return null;
        }
        
        Predicate<AisPacket> p = AisPacketFilters.parseExpressionFilter(filters
                .get(0));
        for (int i = 1; i < filters.size(); i++) {
            p = p.and(AisPacketFilters.parseExpressionFilter(filters.get(i)));
        }
        return p;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("QueryParameterHelper{");
        sb.append("area=").append(area);
        sb.append(", interval=").append(interval);
        sb.append(", kmlSnapshotAt=").append(kmlSnapshotAt);
        sb.append(", limit=").append(limit);
        sb.append(", minDistance=").append(minDistance);
        sb.append(", minDuration=").append(minDuration);
        sb.append(", primaryMmsi=").append(primaryMmsi);
        sb.append(", secondaryMmsi=").append(secondaryMmsi);
        sb.append(", title='").append(title).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", interpolationStepSecs=").append(interpolationStepSecs);
        sb.append(", timeToRun=").append(timeToRun);
        sb.append(", mmsis=");
        if (mmsis == null) {
            sb.append("null");
        } else {
            sb.append('[');
            for (int i = 0; i < mmsis.length; ++i) {
                sb.append(i == 0 ? "" : ", ").append(mmsis[i]);
            }
            sb.append(']');
        }
        sb.append(", outputSink=").append(outputSink);
        sb.append(", sourceFilter=").append(sourceFilter);
        sb.append(", uriInfo=").append(uriInfo);
        sb.append(", jobId='").append(jobId).append('\'');
        sb.append(", createSituationFolder=").append(createSituationFolder);
        sb.append(", createMovementsFolder=").append(createMovementsFolder);
        sb.append(", createTracksFolder=").append(createTracksFolder);
        sb.append('}');
        return sb.toString();
    }
}

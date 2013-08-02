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

import static dk.dma.ais.view.rest.resources.util.QueryParameterParser.findArea;
import static dk.dma.ais.view.rest.resources.util.QueryParameterParser.getOutputSink;
import static dk.dma.ais.view.rest.resources.util.QueryParameterParser.getSourceFilter;
import static dk.dma.commons.web.rest.UriQueryUtil.getOneOrZeroParametersOrFail;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.joda.time.Interval;

import com.google.common.primitives.Ints;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.store.AisStoreQueryBuilder;
import dk.dma.ais.view.rest.resources.util.QueryParameterParser;
import dk.dma.commons.util.Iterables;
import dk.dma.commons.web.rest.StreamingUtil;
import dk.dma.commons.web.rest.UriQueryUtil;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.util.function.Predicate;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class AisStoreResource extends AbstractViewerResource {

    @GET
    @Produces("text/plain")
    @Path("/store")
    public StreamingOutput foo(@Context final UriInfo info) {
        // find all parameters, no reason to start a query, if some of the parameters
        // from the user is invalid.
        Set<Integer> mmsi = new HashSet<>(UriQueryUtil.getParametersAsInt(info, "mmsi"));
        Area area = findArea(info);
        Interval interval = QueryParameterParser.findInterval(info);
        Predicate<? super AisPacket> f = getSourceFilter(info);
        String limit = getOneOrZeroParametersOrFail(info, "limit", null);

        AisStoreQueryBuilder b;
        if (mmsi.size() > 0) {
            b = AisStoreQueryBuilder.forMmsi(Ints.toArray(mmsi));
        } else if (area != null) {
            b = AisStoreQueryBuilder.forArea(area);
        } else {
            b = AisStoreQueryBuilder.forTime();
        }

        Iterable<AisPacket> query = getStore().execute(b.setInterval(interval));
        if (f != Predicate.TRUE) {
            query = Iterables.filter(query, f);
        }

        if (limit != null) {
            query = com.google.common.collect.Iterables.limit(query, Integer.parseInt(limit));
        }
        return StreamingUtil.createStreamingOutput(query, getOutputSink(info));
    }
}

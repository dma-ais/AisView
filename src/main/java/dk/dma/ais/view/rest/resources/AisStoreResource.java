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

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.google.common.primitives.Ints;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.view.rest.resources.util.UriQueryUtil;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;

/**
 * 
 * @author Kasper Nielsen
 */
@Path("/")
public class AisStoreResource extends AbstractViewerResource {

    private Area findArea(UriInfo info) {
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

    @GET
    @Produces("text/plain")
    @Path("/store")
    public String foo(@Context final UriInfo info) {
        long start = 0;
        long stop = Integer.MAX_VALUE;
        Set<Integer> mmsi = new HashSet<>(UriQueryUtil.getParametersAsInt(info, "mmsi"));
        Area area = findArea(info);

        final Iterable<AisPacket> query;
        if (mmsi.size() > 0) {
            query = getStore().findForMmsi(start, stop, Ints.toArray(mmsi));
        } else if (area != null) {
            query = getStore().findForArea(area, start, stop);
        } else {
            query = getStore().findForTime(start, stop);
        }

        return "hi\n";
    }
}

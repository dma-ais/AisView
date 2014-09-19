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
package dk.dma.ais.view.common.util;

import java.util.function.Predicate;

import dk.dma.ais.packet.AisPacketFilters;
import dk.dma.ais.tracker.TargetInfo;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;

public class TargetInfoFilters {
    
    /**
     * TargetInfo predicate for boundingbox filtering
     * 
     * @param bbox
     * @return
     */
    public static Predicate<TargetInfo> filterOnBoundingBox(final BoundingBox bbox) {
        return new Predicate<TargetInfo>() {
            @Override
            public boolean test(TargetInfo arg0) {
                if (arg0.hasPositionInfo()) {
                    Position p = arg0.getPosition();
                    if (p != null
                            && Position.isValid(p.getLatitude(),
                                    p.getLongitude())) {
                        
                        return bbox.contains(p);
                    }
                }
                return false;
            }
        };
    }

    /**
     * Filter on TTL using TargetInfo. 
     * 
     * @param ttl
     * @return
     */
    public static Predicate<TargetInfo> filterOnTTL(final int ttl) {
        return new Predicate<TargetInfo>() {
            @Override
            public boolean test(TargetInfo arg0) {
                long newest = Math.max(arg0.getPositionTimestamp(),
                        arg0.getStaticTimestamp());
                long elapsed = System.currentTimeMillis() - newest;
                return elapsed / 1000 < ttl;
            }

        };
    }

    /**
     * Overloaded bounding box filtering using points
     * 
     * @param pointA
     * @param pointB
     * @return
     */
    public static Predicate<? super TargetInfo> filterOnBoundingBox(
            final Position pointA, final Position pointB) {
        return filterOnBoundingBox(BoundingBox.create(pointA, pointB,
                CoordinateSystem.GEODETIC));
    }

    public static Predicate<TargetInfo> filterOnHasStatic() {
        return e-> e.hasStaticInfo();
    }

    public static Predicate<? super TargetInfo> filterOnHasPosition() {
        return e-> e.hasPositionInfo();
    }


}

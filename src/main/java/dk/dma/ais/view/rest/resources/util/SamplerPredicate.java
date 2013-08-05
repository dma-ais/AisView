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

import dk.dma.ais.packet.AisPacket;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.util.function.Predicate;

/**
 * A non thread-safe predicate (statefull) that can be used sample duration/distance.
 * 
 * @author Kasper Nielsen
 */
class SamplerPredicate extends Predicate<AisPacket> {

    /** The latest received position that was accepted, or null if no position has been received. */
    Position latestPosition;

    /** The latest time stamp that was accepted, or null if no packets has been received. */
    Long latestTimestamp;

    /** If non-null the minimum distance traveled in meters between updates. */
    final Integer minDistanceInMeters;

    /** If non-null the minimum duration in milliseconds between updates. */
    final Long minDurationInMS;

    SamplerPredicate(Integer minDistanceInMeters, Long minDurationInMS) {
        this.minDistanceInMeters = minDistanceInMeters;
        this.minDurationInMS = minDurationInMS;
    }

    /** {@inheritDoc} */
    @Override
    public boolean test(AisPacket p) {
        Position pos = p.tryGetAisMessage().getValidPosition(); // This is ok, only valid position messsages get to here
        boolean updateDistance = minDistanceInMeters != null
                && (latestPosition == null || latestPosition.rhumbLineDistanceTo(pos) >= minDistanceInMeters);
        boolean updateDuration = minDurationInMS != null
                && (latestTimestamp == null || p.getBestTimestamp() - latestTimestamp >= minDurationInMS);
        if (!updateDistance && !updateDuration) {
            return false;
        }
        // update latest positions
        latestPosition = pos;
        latestTimestamp = p.getBestTimestamp();
        return true;
    }
}

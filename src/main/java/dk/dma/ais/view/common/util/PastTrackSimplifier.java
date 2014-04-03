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

import java.util.Arrays;
import java.util.List;

import dk.dma.ais.data.AisVesselPosition;
import dk.dma.ais.data.IPastTrack;
import dk.dma.ais.data.PastTrackPoint;


/**
 * 
 * @author Jens Tuxen
 *
 */
public final class PastTrackSimplifier {
    final PastTrackPointExtractor pte = new PastTrackPointExtractor();
    
    public IPastTrack simplifyPastTrack(IPastTrack pt, double tolerance) {
        List<PastTrackPoint> points = pt.getPoints();
        PastTrackPoint[] arr = points.toArray(new PastTrackPoint[points.size()]);
        
        SimplifyRhumbline simplify = new SimplifyRhumbline(new PastTrackPoint[0],pte);
        PastTrackPoint[] arr2 = simplify.simplify(arr, tolerance, true);
        return new ImmutablePastTrack(Arrays.asList(arr2));
    }
    
    private class ImmutablePastTrack implements IPastTrack {
        List<PastTrackPoint> points;
        
        ImmutablePastTrack(List<PastTrackPoint> points) {
            this.points = points;
            
        }

        @Override
        public void addPosition(AisVesselPosition vesselPosition, int minDist) {
            throw new UnsupportedOperationException("This PastTrack is Immutable");
        }

        @Override
        public void cleanup(int ttl) {
            throw new UnsupportedOperationException("This PastTrack is Immutable");
        }

        @Override
        public List<PastTrackPoint> getPoints() {
            return points;
        }
        
    }
    
    
}

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
        PastTrackPoint[] arr2 = simplify.simplify(arr, tolerance, false);
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

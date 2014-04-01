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

import java.util.ArrayList;
import java.util.List;

import com.goebl.simplify.PointExtractor;
import com.goebl.simplify.Simplify;

import dk.dma.ais.data.PastTrackPoint;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;

public class SimplifyRhumbline extends Simplify<PastTrackPoint> {
    PointExtractor<PastTrackPoint> pe;
    private PastTrackPoint[] sampleArray;

    public SimplifyRhumbline(PastTrackPoint[] sampleArray,
            PointExtractor<PastTrackPoint> pe) {
        super(sampleArray, pe);
        this.pe = pe;
        this.sampleArray = sampleArray;
    }

    @Override
    public double getSquareDistance(PastTrackPoint p1, PastTrackPoint p2) {
        Position pos1 = Position.create(pe.getX(p1), pe.getY(p1));
        Position pos2 = Position.create(pe.getX(p2), pe.getY(p2));

        double dist = pos1.distanceTo(pos2, CoordinateSystem.CARTESIAN);

        return dist * dist;
    }

    /*
     * @Override public double getSquareSegmentDistance(PastTrackPoint p0,
     * PastTrackPoint p1, PastTrackPoint p2) { throw new
     * NotImplementedException("Try RadialDistance"); }
     */

    public PastTrackPoint[] simplifyLowQuality(PastTrackPoint[] points,
            double sqTolerance) {
        PastTrackPoint point = null;
        PastTrackPoint prevPoint = points[0];

        List<PastTrackPoint> newPoints = new ArrayList<PastTrackPoint>();
        newPoints.add(prevPoint);

        for (int i = 1; i < points.length; ++i) {
            point = points[i];

            if (getSquareDistance(point, prevPoint) > sqTolerance) {
                newPoints.add(point);
                prevPoint = point;
            }
        }

        if (prevPoint != point) {
            newPoints.add(point);
        }

        return newPoints.toArray(sampleArray);
    }

    @Override
    public double getSquareSegmentDistance(PastTrackPoint p0,
            PastTrackPoint p1, PastTrackPoint p2) {
        double x0, y0, x1, y1, x2, y2, dx, dy, t;

        x1 = pe.getX(p1);
        y1 = pe.getY(p1);
        x2 = pe.getX(p2);
        y2 = pe.getY(p2);
        x0 = pe.getX(p0);
        y0 = pe.getY(p0);

        // dx = x2 - x1;
        // dy = y2 - y1;
        dx = Position.create(x1, y1)
                .rhumbLineDistanceTo(Position.create(x2, y1));
        dy = Position.create(x1, y2)
                .rhumbLineDistanceTo(Position.create(x1, y1));

        if (dx != 0.0d || dy != 0.0d) {
            double x0mx1 = Position.create(x0, y0).rhumbLineDistanceTo(
                    Position.create(x1, y0));
            double y0my1 = Position.create(x0, y0).rhumbLineDistanceTo(
                    Position.create(x0, y1));

            t = ((x0mx1 * dx) + (y0my1 * dy)) / (dx * dx + dy * dy);

            if (t > 1.0d) {
                x1 = x2;
                y1 = y2;
            } else if (t > 0.0d) {
                x1 += dx * t;
                y1 += dy * t;
            }
        }

        dx = x0 - x1;
        dy = y0 - y1;

        return dx * dx + dy * dy;
    }

}

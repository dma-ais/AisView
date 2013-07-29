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
package dk.dma.ais.view;

import dk.dma.ais.packet.AisPacketStream;
import dk.dma.ais.reader.AisReaderGroup;
import dk.dma.ais.store.old.AisStoreOld;
import dk.dma.ais.view.tracker.TargetTracker;

/**
 * 
 * @author Kasper Nielsen
 */
public class AisViewer {

    final AisStoreOld store;

    final TargetTracker targetTracker;

    /** All readers, null if we do not support live targets. */
    final AisReaderGroup g;

    AisViewer(AisReaderGroup readerGroup, AisStoreOld store) {
        this.g = readerGroup;
        this.store = store;
        if (readerGroup == null) {
            targetTracker = null;
        } else {
            targetTracker = new TargetTracker();
        }
    }

    public TargetTracker getTracker() {
        if (targetTracker == null) {
            throw new UnsupportedOperationException();
        }
        return targetTracker;
    }

    public AisPacketStream stream() {
        if (g == null) {
            throw new UnsupportedOperationException();
        }
        return g.stream();
    }

    /**
     * @return
     */
    public AisStoreOld getAisStore() {
        if (store == null) {
            throw new UnsupportedOperationException();
        }
        return store;
    }
}

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
import dk.dma.ais.store.AisStoreConnection;
import dk.dma.ais.tracker.TargetTracker;

/**
 * 
 * @author Kasper Nielsen
 */
public class AisViewer {

    final AisStoreConnection connection;

    final TargetTracker targetTracker;

    /** All readers, null if we do not support live targets. */
    final AisReaderGroup readers;

    AisViewer(AisReaderGroup readers, AisStoreConnection connection) {
        this.readers = readers;
        this.connection = connection;
        if (readers == null) {
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

    public AisPacketStream newLiveStream() {
        if (readers == null) {
            throw new UnsupportedOperationException();
        }
        return readers.stream();
    }

    /**
     * @return
     */
    public AisStoreConnection getAisStore() {
        if (connection == null) {
            throw new UnsupportedOperationException();
        }
        return connection;
    }
}

/*
 * Copyright (c) 2008 Kasper Nielsen.
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
package dk.dma.ais.view;

import dk.dma.ais.packet.AisPacketStream;
import dk.dma.ais.reader.AisReaderGroup;
import dk.dma.ais.store.AisStore;
import dk.dma.ais.view.target.TargetTracker;

/**
 * 
 * @author Kasper Nielsen
 */
public class AisViewer {

    final AisStore store;

    final TargetTracker targetTracker;

    /** All readers, null if we do not support live targets. */
    final AisReaderGroup g;

    AisViewer(AisReaderGroup readerGroup, AisStore store) {
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
    public AisStore getAisStore() {
        if (store == null) {
            throw new UnsupportedOperationException();
        }
        return store;
    }
}

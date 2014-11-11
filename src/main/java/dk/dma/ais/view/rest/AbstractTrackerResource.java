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
package dk.dma.ais.view.rest;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.core.UriInfo;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.tracker.TargetInfo;
import dk.dma.ais.tracker.TargetTracker;
import dk.dma.commons.web.rest.AbstractResource;

/**
 * 
 * @author Ole Bakman Borup
 *
 */
public class AbstractTrackerResource extends AbstractResource {

    public AbstractTrackerResource() {
        super();
    }

    /**
     * Helper function for applying filters from Stream to Iterable.
     * 
     * @param streamPackets
     * @param qh
     * @return
     */
    protected Iterable<AisPacket> applyFilters(Stream<AisPacket> streamPackets, QueryParameterHelper qh) {
        // reapplying filters on packet stream
        Iterable<AisPacket> packets = (Iterable<AisPacket>) streamPackets::iterator;

        // filters
        packets = qh.applyPacketFilter(packets);

        return packets;
    }

    protected Stream<AisPacket> getPacketStream(UriInfo info, QueryParameterHelper qh) {
        Predicate<AisPacketSource> predSource = qh.getSourcePredicate();
        predSource = (predSource == null) ? e -> true : predSource;

        Predicate<TargetInfo> predTarget = qh.getTargetPredicate();
        predTarget = (predTarget == null) ? e -> true : predTarget;

        predTarget = (qh.getArea() != null) ? qh.getTargetAreaFilter() : predTarget;

        TargetTracker tt = get(TargetTracker.class);

        Stream<TargetInfo> s = tt.findTargets8(predSource, predTarget);
        Stream<AisPacket[]> sPackets = s.map(e -> e.getPackets()).filter(o -> o != null);

        final ConcurrentLinkedDeque<AisPacket> packets = new ConcurrentLinkedDeque<AisPacket>();
        sPackets.sequential().forEach(e -> {
            for (int i = 0; i < e.length; i++) {
                packets.add(e[i]);
            }
        });

        return packets.stream();
    }

}

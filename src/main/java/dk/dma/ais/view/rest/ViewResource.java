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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import dk.dma.ais.data.AisTarget;
import dk.dma.ais.data.AisVesselTarget;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.view.rest.json.ViewTarget;

/**
 * Resource delivering real-time image of target positions
 * 
 * @author Ole Bakman Borup
 */
@Path("/view")
public class ViewResource extends AbstractTrackerResource {

    public ViewResource() {
        super();
    }

    @GET
    @Path("/vessels/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ViewTarget> getVessels(@Context UriInfo info) {
        QueryParameterHelper qh = new QueryParameterHelper(info);
        Stream<AisPacket> packets = getPacketStream(info, qh);
        Iterable<AisPacket> filteredPackets = applyFilters(packets, qh);
        
        // TODO separate method to get AisTargets from packets
        Map<Integer, AisTarget> targets = new HashMap<>();
        for (AisPacket packet : filteredPackets) {
            AisMessage message = packet.tryGetAisMessage();
            if (message == null || !AisTarget.isTargetDataMessage(message)) {
                continue;
            }
            int mmsi = message.getUserId();
            // Get existing target
            AisTarget target = targets.get(mmsi);
            if (target == null) {
                target = AisTarget.createTarget(message);
                targets.put(mmsi, target);
            }
            try {
                target.update(message);
            } catch (IllegalArgumentException e) {
                // Trying to update target with report of different type of target.
                // Replace target with new target
                target = AisTarget.createTarget(message);
                target.update(message);
                targets.put(mmsi, target);
            }
        }
        
        List<ViewTarget> list = new ArrayList<>();
        for (Integer mmsi : targets.keySet()) {
            AisTarget target = targets.get(mmsi);
            if (target instanceof AisVesselTarget) {
                list.add(ViewTarget.create((AisVesselTarget)target));
            }            
        }       
        
        // TODO Filter on ttl
        
        return list;
    }
    
}

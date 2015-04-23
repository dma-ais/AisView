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
package dk.dma.ais.view.rest.json;

import dk.dma.ais.data.AisClassAPosition;
import dk.dma.ais.data.AisClassAStatic;
import dk.dma.ais.data.AisClassBStatic;
import dk.dma.ais.data.AisVesselPosition;
import dk.dma.ais.data.AisVesselStatic;
import dk.dma.ais.data.AisVesselTarget;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisStaticCommon;
import dk.dma.ais.message.AisTargetType;
import dk.dma.ais.message.ShipTypeCargo;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.tracker.targetTracker.TargetInfo;

import java.util.ArrayList;
import java.util.Locale;

public class VesselList extends BaseVesselList {
    
    public VesselList() {
        super();
    }
    
    public void addTarget(AisVesselTarget vesselTarget, int anonId) {
        AisVesselPosition pos = vesselTarget.getVesselPosition();
        AisVesselStatic statics = vesselTarget.getVesselStatic();
        if (pos == null || pos.getPos() == null) {
            return;
        }
                        
        Double cog = pos.getCog();
        Double sog = pos.getSog();
        Double lat = pos.getPos().getLatitude();
        Double lon = pos.getPos().getLongitude();
        
        String vesselClass;
        ShipTypeCargo shipTypeCargo = null;
        if (statics != null) {
            shipTypeCargo = statics.getShipTypeCargo();
        }
        
        Byte navStatus = null;
        if (pos instanceof AisClassAPosition) {
            navStatus = ((AisClassAPosition)pos).getNavStatus();
            vesselClass = "A";
        } else {
            vesselClass = "B";
        }
        
        if (cog == null) {
            cog = 0d;
        }
        if (sog == null) {
            sog = 0d;
        }
        
        // Round cog to nearest 10
        long cogL = Math.round(cog / 10.0) * 10;
        if (cogL == 360) {
            cogL = 0;
        }
        
        ArrayList<String> list = new ArrayList<String>();
        
        list.add(Long.toString(cogL));
        list.add(String.format(Locale.US, "%.5f", lat));
        list.add(String.format(Locale.US, "%.5f", lon));
        list.add(vesselClass);
        ShipTypeMapper.ShipTypeColor color = ShipTypeMapper.ShipTypeColor.GREY;
        if (shipTypeCargo != null) {
            color = shipTypeMapper.getColor(shipTypeCargo.getShipType());
        }
        list.add(Integer.toString(color.ordinal()));
        
        list.add((navStatus != null && (navStatus == 1 || navStatus ==5)) ? "1" : "0");
        
        list.add(Long.toString(vesselTarget.getMmsi()));        
        list.add((statics == null) ? "N/A" : statics.getName());
        list.add((statics == null) ? "N/A" : statics.getCallsign());
        String imoNo = "N/A";
        if ((statics != null) && (statics instanceof AisClassAStatic)) {
            Integer num = ((AisClassAStatic)statics).getImoNo();  
            if (num != null) {
                imoNo = Integer.toString(num);
            }
        }
        list.add(imoNo);
        
        
        vessels.put(anonId, list);    
        vesselCount++;
    }
    
    @Override
    public void addTarget(TargetInfo vesselTarget, int anonId) {


        if (!vesselTarget.hasPositionInfo() || vesselTarget.getPosition() == null) {
            return;
        }
        
        float cog = vesselTarget.getCog()/10.0f;
        //float sog = vesselTarget.getSog();
        Double lat = vesselTarget.getPosition().getLatitude();
        Double lon = vesselTarget.getPosition().getLongitude();

        
        String vesselClass;
        ShipTypeCargo shipTypeCargo = new ShipTypeCargo(vesselTarget.getStaticShipType());
        

        Byte navStatus = vesselTarget.getNavStatus();        
        
        vesselClass = vesselTarget.getTargetType().toString();


        // Round cog to nearest 10
        long cogL = Math.round(cog / 10.0) * 10;
        if (cogL == 360) {
            cogL = 0;
        }

        ArrayList<String> list = new ArrayList<String>();
        
        list.add(Long.toString(cogL));
        list.add(String.format(Locale.US, "%.5f", lat));
        list.add(String.format(Locale.US, "%.5f", lon));
        list.add(vesselClass);
        ShipTypeMapper.ShipTypeColor color = ShipTypeMapper.ShipTypeColor.GREY;
        if (shipTypeCargo != null) {
            color = shipTypeMapper.getColor(shipTypeCargo.getShipType());
        }
        list.add(Integer.toString(color.ordinal()));
        
        list.add((navStatus != null && (navStatus == 1 || navStatus ==5)) ? "1" : "0");
        
        list.add(Long.toString(vesselTarget.getMmsi()));     
        
        String name = "N/A";
        String callsign = "N/A";
        String imoNo = "N/A";
                
        
        AisTargetType att = vesselTarget.getTargetType();
        if (vesselTarget.hasStaticInfo() && (att == AisTargetType.A || att == AisTargetType.B)) {
            AisVesselStatic avs = (att == AisTargetType.A) ? new AisClassAStatic() : new AisClassBStatic();            
            
            for (AisPacket p: vesselTarget.getStaticPackets()) {
                AisMessage m = p.tryGetAisMessage();
                
                if (m != null && m instanceof AisMessage5) {
                    AisMessage5 am5 = (AisMessage5)m;
                    imoNo = (am5.getImo() > 0) ? Long.toString(am5.getImo()) : "N/A";
                    avs.update(am5);
                } else if (m != null && m instanceof AisStaticCommon ) {
                    avs.update((AisStaticCommon)m);
                }
            }
            
            name = AisMessage.trimText(avs.getName());
            callsign = AisMessage.trimText(avs.getCallsign());

            
        }

        list.add(name);
        list.add(callsign);
        list.add(imoNo);
        
        vessels.put(anonId, list);    
        vesselCount++;
        
    }

}

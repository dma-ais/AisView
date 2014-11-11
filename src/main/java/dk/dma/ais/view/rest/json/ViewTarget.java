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
import dk.dma.ais.data.AisClassATarget;
import dk.dma.ais.data.AisTargetDimensions;
import dk.dma.ais.data.AisVesselPosition;
import dk.dma.ais.data.AisVesselStatic;
import dk.dma.ais.data.AisVesselTarget;
import dk.dma.ais.message.AisTargetType;
import dk.dma.ais.message.NavigationalStatus;
import dk.dma.enav.model.geometry.Position;

/**
 * 
 * @author Ole Bakman Borup
 *
 */
public class ViewTarget {

    int mmsi;
    AisTargetType targetType;
    String country;
    long lastReport;
    Double lat;
    Double lon;
    Double cog;
    Double sog;
    Double heading;
    Double rot;
    Integer length;
    Integer width;
    String name;
    String callsign;
    String imoNo;
    String destination;
    Double draught;
    String navStatus;
    Boolean moored;
    Long eta;
    String sourceType;
    String vesselType;
    String cargo;

    private ViewTarget() {
    }
    
    public static ViewTarget create(AisVesselTarget target) {
        ViewTarget vt = new ViewTarget();
        vt.mmsi = target.getMmsi();
        if (target instanceof AisClassATarget) {
            vt.targetType = AisTargetType.A;
        } else {
            vt.targetType = AisTargetType.B;
        }
        if (target.getCountry() != null) {
            vt.country = target.getCountry().getTwoLetter();
        }
        vt.lastReport = target.getLastReport().getTime();
        AisVesselPosition pos = target.getVesselPosition();
        if (pos != null) {
            if (pos.getPos() != null) {
                vt.lat = pos.getPos().getLatitude();
                vt.lon = pos.getPos().getLongitude();
            }
            vt.cog = pos.getCog();
            vt.sog = pos.getSog();
            vt.heading = pos.getHeading();            
            if (pos instanceof AisClassAPosition) {
                AisClassAPosition posA = (AisClassAPosition)pos;
                vt.rot = posA.getRot();
                NavigationalStatus navigationalStatus = NavigationalStatus.get(posA.getNavStatus());
                vt.navStatus = navigationalStatus.prettyStatus();
                vt.moored = posA.getNavStatus() == 1 || posA.getNavStatus() == 5;
            }
        }
        
        AisVesselStatic sta = target.getVesselStatic();
        if (sta != null) {
            vt.name = sta.getName();
            vt.callsign = sta.getCallsign();
            AisTargetDimensions dim = sta.getDimensions();
            if (dim != null) {
                vt.length = dim.getDimBow() + dim.getDimStern();
                vt.width = dim.getDimPort() + dim.getDimStarboard();
            }
            vt.vesselType = sta.getShipTypeCargo().prettyType();
            vt.cargo = sta.getShipTypeCargo().prettyCargo();
            if (sta instanceof AisClassAStatic) {
                AisClassAStatic staA = (AisClassAStatic)sta;
                vt.destination = staA.getDestination();
                if (staA.getEta() != null) {
                    vt.eta = staA.getEta().getTime();
                }
                vt.draught = staA.getDraught();
            }            
        }
        
        return vt;
    }

    public int getMmsi() {
        return mmsi;
    }

    public AisTargetType getTargetType() {
        return targetType;
    }

    public String getCountry() {
        return country;
    }

    public long getLastReport() {
        return lastReport;
    }

    public Double getLat() {
        return lat;
    }
    
    public Double getLon() {
        return lon;
    }

    public Double getCog() {
        return cog;
    }

    public Double getSog() {
        return sog;
    }

    public Double getHeading() {
        return heading;
    }

    public Double getRot() {
        return rot;
    }

    public Integer getLength() {
        return length;
    }

    public Integer getWidth() {
        return width;
    }

    public String getName() {
        return name;
    }

    public String getCallsign() {
        return callsign;
    }

    public String getImoNo() {
        return imoNo;
    }

    public String getDestination() {
        return destination;
    }

    public Double getDraught() {
        return draught;
    }

    public String getNavStatus() {
        return navStatus;
    }

    public Boolean getMoored() {
        return moored;
    }

    public Long getEta() {
        return eta;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getVesselType() {
        return vesselType;
    }
    
    public String getCargo() {
        return cargo;
    }
    
}

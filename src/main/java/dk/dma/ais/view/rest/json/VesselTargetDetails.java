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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import dk.dma.ais.data.AisClassAPosition;
import dk.dma.ais.data.AisClassAStatic;
import dk.dma.ais.data.AisClassBStatic;
import dk.dma.ais.data.AisClassBTarget;
import dk.dma.ais.data.AisTargetDimensions;
import dk.dma.ais.data.AisVesselPosition;
import dk.dma.ais.data.AisVesselStatic;
import dk.dma.ais.data.AisVesselTarget;
import dk.dma.ais.data.IPastTrack;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage18;
import dk.dma.ais.message.AisMessage3;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisStaticCommon;
import dk.dma.ais.message.AisTargetType;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.message.NavigationalStatus;
import dk.dma.ais.message.ShipTypeCargo;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketSource;
import dk.dma.ais.tracker.TargetInfo;
import dk.dma.enav.model.geometry.Position;

public class VesselTargetDetails {    
    
    protected long id;
    protected long mmsi;
    protected String vesselClass;
    protected String lastReceived;
    protected long currentTime;
    protected String lat;
    protected String lon;
    protected String cog;
    protected boolean moored;
    protected String vesselType = "N/A";
    protected String length = "N/A";
    protected String width = "N/A";
    protected String sog;
    protected String name = "N/A";
    protected String callsign = "N/A";
    protected String imoNo = "N/A";
    protected String cargo = "N/A";
    protected String country;
    protected String draught = "N/A";
    protected String heading = "N/A";
    protected String rot = "N/A";
    protected String destination = "N/A";
    protected String navStatus = "N/A";
    protected String eta = "N/A";
    protected String posAcc = "N/A";
    protected String sourceType;
    protected String sourceSystem;
    protected String sourceRegion;
    protected String sourceBs;
    protected String sourceCountry;
    protected String pos;
    protected IPastTrack pastTrack;

    public VesselTargetDetails(AisVesselTarget target, AisPacketSource aisSource, int anonId, IPastTrack pastTrack) {        
        AisVesselPosition pos = target.getVesselPosition();
        
        if (pos == null || pos.getPos() == null) {
            return;
        }
        AisClassAPosition classAPos = null;
        if (pos instanceof AisClassAPosition) {
            classAPos = (AisClassAPosition)pos;
        }
        
        this.pastTrack = pastTrack;
                        
        this.currentTime = System.currentTimeMillis();
        this.id = anonId;
        this.mmsi = target.getMmsi();
        this.vesselClass = (target instanceof AisClassBTarget) ? "B" : "A";
        this.lastReceived = formatTime(currentTime - target.getLastReport().getTime());
        this.lat = latToPrintable(pos.getPos().getLatitude());
        this.lon = lonToPrintable(pos.getPos().getLongitude());
        this.cog = formatDouble(pos.getCog(), 0);        
        this.heading = formatDouble(pos.getHeading(), 1);
        this.sog = formatDouble(pos.getSog(), 1);    
        

        if (target.getCountry() != null) {
            this.country = target.getCountry().getName();
        } else {
            this.country ="N/A";
        }
        
        this.sourceType = (aisSource.getSourceType() == null) ? "N/A" : aisSource.getSourceType().encode();
        
        this.sourceSystem = aisSource.getSourceId();
        if (this.sourceSystem == null) {
            this.sourceSystem = "N/A";
        }
        this.sourceRegion = aisSource.getSourceRegion();
        if (this.sourceRegion == null) {
            this.sourceRegion = "N/A";
        }
        
        if (aisSource.getSourceBaseStation() >= 1) {
            this.sourceBs = Integer.toString(aisSource.getSourceBaseStation());
        } else {
            this.sourceBs = "N/A";
        }
        
        if (aisSource.getSourceCountry() != null) {
            this.sourceCountry = aisSource.getSourceCountry().getName();
        } else {
            this.sourceCountry = "N/A";
        }
        
        // Class A position
        if (classAPos != null) {
            //TODO fix changed method.
            NavigationalStatus navigationalStatus = NavigationalStatus.get(classAPos.getNavStatus());
            this.navStatus = navigationalStatus.prettyStatus();
            this.moored = classAPos.getNavStatus() == 1 || classAPos.getNavStatus() == 5;
            this.rot = formatDouble(classAPos.getRot(), 1);
        }
                
        if (pos.getPosAcc() == 1) {
            this.posAcc = "High";
        } else {
            this.posAcc = "Low";
        }
        
        this.pos = latToPrintable(pos.getPos().getLatitude()) + " - " + lonToPrintable(pos.getPos().getLongitude());
        
        
        AisVesselStatic statics = target.getVesselStatic();
        if (statics == null) {
            return;
        }
        AisClassAStatic classAStatics = null;
        if (statics instanceof AisClassAStatic) {
            classAStatics = (AisClassAStatic)statics;
        }
        
        AisTargetDimensions dim = statics.getDimensions();
        if (dim != null) {
            this.length = Integer.toString(dim.getDimBow() + dim.getDimStern());
            this.width = Integer.toString(dim.getDimPort() + dim.getDimStarboard());
        }
        if (statics.getShipTypeCargo() != null) {
            this.vesselType = statics.getShipTypeCargo().prettyType();
            this.cargo = statics.getShipTypeCargo().prettyCargo();
        }
        this.name = statics.getName();
        this.callsign = statics.getCallsign();        
        // Class A statics
        if (classAStatics != null) {
            if (classAStatics.getImoNo() != null) {
                this.imoNo = Integer.toString(classAStatics.getImoNo()); 
            } else {
                this.imoNo = "N/A";
            }                    
            this.destination = (classAStatics.getDestination() != null) ? classAStatics.getDestination() : "N/A";
            this.draught = (classAStatics.getDraught() != null) ? formatDouble((double)classAStatics.getDraught(), 1) : "N/A";
            this.eta = getISO8620(classAStatics.getEta());
        }        

    }
    
    public VesselTargetDetails(TargetInfo target, AisPacketSource aisSource, int anonId, IPastTrack pastTrack) {
        if (!target.hasPositionInfo() || target.getPosition() == null) {
            return;
        }
        
        Position pos = target.getPosition();
        
        this.pastTrack = pastTrack;
        
        long lastTime = Math.max(target.getPositionTimestamp(), target.getStaticTimestamp());
                        
        this.currentTime = System.currentTimeMillis();
        this.id = anonId;
        this.mmsi = target.getMmsi();
        this.vesselClass = (target.getTargetType() == AisTargetType.A) ? "A" : "B";
        this.lastReceived = formatTime(currentTime - lastTime);
        this.lat = latToPrintable(pos.getLatitude());
        this.lon = lonToPrintable(pos.getLongitude());
        this.cog = formatDouble((double) target.getCog(), 0);        
        this.heading = formatDouble((double)target.getHeading(), 1);
        this.sog = formatDouble((double)target.getSog(), 1);    

        this.country = (target.getCountry() == null) ? "N/A" : target.getCountry().getName();
        this.sourceType = (aisSource.getSourceType() == null) ? "N/A" : aisSource.getSourceType().encode();
        this.sourceSystem = (aisSource.getSourceId() == null) ? "N/A" : aisSource.getSourceId();
        this.sourceRegion = (aisSource.getSourceRegion() == null) ? "N/A": aisSource.getSourceRegion();        
        this.sourceBs = (aisSource.getSourceBaseStation() <= 0) ? "N/A" : Integer.toString(aisSource.getSourceBaseStation());
        
        this.sourceCountry = (aisSource.getSourceCountry() == null) ? "N/A" : aisSource.getSourceCountry().getName();
        // Class A position
        if (target.hasStaticInfo() && target.getTargetType() == AisTargetType.A) {
            //TODO fix changed method.
            NavigationalStatus navigationalStatus = NavigationalStatus.get(target.getNavStatus());
            this.navStatus = navigationalStatus.prettyStatus();
            this.moored = target.getNavStatus() == 1 || target.getNavStatus() == 5;

        }
        
        //expensive?
        AisMessage tMessage = target.getPositionPacket().tryGetAisMessage();
        if (tMessage instanceof IVesselPositionMessage) {
            IVesselPositionMessage m = (IVesselPositionMessage)tMessage;
            
            this.posAcc = (m.getPosAcc() == 1) ? "High" : "Low";
            
            if (m instanceof AisMessage3) {
                AisMessage3 i = (AisMessage3)m;
                this.rot = (i.getRot() > -1) ? "N/A": Integer.toString(i.getRot(), 1);
            }
            
        } else {
            this.posAcc = "Low";
        }
        

        this.pos = latToPrintable(pos.getLatitude()) + " - " + lonToPrintable(pos.getLongitude());
        
        
        AisTargetType att = target.getTargetType();
        if (target.hasStaticInfo() && (att == AisTargetType.A || att == AisTargetType.B)) {
            List<AisVesselStatic> avsList = new ArrayList<AisVesselStatic>();
            
            for (AisPacket p: target.getPackets()) {
                AisVesselStatic avs = (att == AisTargetType.A) ? new AisClassAStatic() : new AisClassBStatic();
                
                AisMessage m = p.tryGetAisMessage();
                
                if (m != null) {
                    if (m instanceof AisMessage5) {
                        AisMessage5 am5 = (AisMessage5)m;
                        imoNo = (am5.getImo() > 0) ? Long.toString(am5.getImo()) : "N/A";
                    }
                    
                    avs.update(m);
                }
                
                avsList.add(avs);
            }
            
            avsList.forEach(avs -> {
                
                if (avs.getName() != null) {
                    name = AisMessage.trimText(avs.getName());
                }
                
                if (avs.getCallsign() != null) {
                    callsign = AisMessage.trimText(avs.getCallsign());
                }
                
                if (avs instanceof AisClassAStatic) {
                    AisClassAStatic acas = (AisClassAStatic) avs;
                    if (acas.getDestination() != null) {
                        this.destination = acas.getDestination();
                    }
                    
                    if (acas.getDraught() != null) {
                        this.draught = Double.toString(acas.getDraught());
                    }

                    if (acas.getEta() != null) {
                        this.eta = getISO8620(acas.getEta());
                    }

                }
                
                if (avs.getDimensions() != null) {
                    AisTargetDimensions dim = avs.getDimensions();
                    this.length = Integer.toString(dim.getDimBow() + dim.getDimStern());
                    this.width = Integer.toString(dim.getDimPort() + dim.getDimStarboard());
                }
  
                
            });
            
            
            if (target.getStaticShipType() > 0) {
                ShipTypeCargo stc = new ShipTypeCargo(target.getStaticShipType());
                this.vesselType = stc.prettyType();
                this.cargo = stc.prettyCargo();
            }
            
        }

    }
        
    /**
     * Anonymize is no longer supported
     */
    public void anonymize() {
        throw new UnsupportedOperationException();
        /*
        this.name = "N/A";
        this.callsign = "N/A";
        this.imoNo = "N/A";
        this.destination = "N/A";
        this.mmsi = 0;
        this.eta = "N/A";
        */
    }
    
    public static String getISO8620(Date date) {
        if (date == null) {
            return "N/A";
        }
        SimpleDateFormat iso8601gmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        iso8601gmt.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
        return iso8601gmt.format(date);
    }
    
    public static String formatTime(Long time) {
        if (time == null) {
            return "N/A";
        }
        long secondInMillis = 1000;
        long minuteInMillis = secondInMillis * 60;
        long hourInMillis = minuteInMillis * 60;
        long dayInMillis = hourInMillis * 24;

        long elapsedDays = time / dayInMillis;
        time = time % dayInMillis;
        long elapsedHours = time / hourInMillis;
        time = time % hourInMillis;
        long elapsedMinutes = time / minuteInMillis;
        time = time % minuteInMillis;
        long elapsedSeconds = time / secondInMillis;

        if (elapsedDays > 0) {
            return String.format("%02d:%02d:%02d:%02d", elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds);
        } else if (elapsedHours > 0) {
            return String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
        } else {
            return String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
        }
    }
    
    public static String latToPrintable(Double lat) {
        if (lat == null) {
            return "N/A";
        }
        String ns = "N";
        if (lat < 0) {
            ns = "S";
            lat *= -1;
        }
        int hours = (int)lat.doubleValue();
        lat -= hours;
        lat *= 60;
        String latStr = String.format(Locale.US, "%3.3f", lat);
        while (latStr.indexOf('.') < 2) {
            latStr = "0" + latStr;
        }        
        return String.format(Locale.US, "%02d %s%s", hours, latStr, ns);
    }
    
    public static String lonToPrintable(Double lon) {
        if (lon == null) {
            return "N/A";
        }
        String ns = "E";
        if (lon < 0) {
            ns = "W";
            lon *= -1;
        }
        int hours = (int)lon.doubleValue();
        lon -= hours;
        lon *= 60;        
        String lonStr = String.format(Locale.US, "%3.3f", lon);
        while (lonStr.indexOf('.') < 2) {
            lonStr = "0" + lonStr;
        }        
        return String.format(Locale.US, "%03d %s%s", hours, lonStr, ns);
    }
    
    public static String formatDouble(Double d, int decimals) {
        if (d == null) {
            return "N/A";
        }
        if (decimals == 0) {
            return String.format(Locale.US, "%d", Math.round(d));
        }
        String format = "%." + decimals + "f";
        return String.format(Locale.US, format, d);
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

    public String getCargo() {
        return cargo;
    }

    public String getCountry() {
        return country;
    }

    public String getDraught() {
        return draught;
    }

    public String getRot() {
        return rot;
    }

    public String getDestination() {
        return destination;
    }

    public String getNavStatus() {
        return navStatus;
    }

    public String getEta() {
        return eta;
    }

    public String getPosAcc() {
        return posAcc;
    }

    public long getMmsi() {
        return mmsi;
    }
    
    public String getVesselClass() {
        return vesselClass;
    }
    
    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }

    public boolean isMoored() {
        return moored;
    }
    
    public String getVesselType() {
        return vesselType;
    }

    public String getLength() {
        return length;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public String getLastReceived() {
        return lastReceived;
    }

    public String getWidth() {
        return width;
    }

    public String getSourceType() {
        return sourceType;
    }
    
    public long getId() {
        return id;
    }

    public String getCog() {
        return cog;
    }

    public String getSog() {
        return sog;
    }

    public String getHeading() {
        return heading;
    }

    public String getPos() {
        return pos;
    }
    
    public String getSourceSystem() {
        return sourceSystem;
    }
    
    public String getSourceRegion() {
        return sourceRegion;
    }
    
    public String getSourceBs() {
        return sourceBs;
    }
    
    public String getSourceCountry() {
        return sourceCountry;
    }
    
    public IPastTrack getPastTrack() {
        return pastTrack;
    }
    
}

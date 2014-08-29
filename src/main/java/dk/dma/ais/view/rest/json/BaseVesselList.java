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

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.concurrent.ThreadSafe;

import dk.dma.ais.data.AisVesselTarget;

@ThreadSafe
public abstract class BaseVesselList {
    
    protected ConcurrentHashMap<Integer, ArrayList<String>> vessels = new ConcurrentHashMap<Integer, ArrayList<String>>();
    protected long currentTime;
    protected int vesselCount;
    protected int inWorldCount;
    
    protected static ShipTypeMapper shipTypeMapper = ShipTypeMapper.getInstance();
    
    public BaseVesselList() {
        currentTime = System.currentTimeMillis();
    }
    
    public abstract void addTarget(AisVesselTarget vesselTarget, int anonId);
    
    public Map<Integer, ArrayList<String>> getVessels() {
        return vessels;
    }
    
    public long getCurrentTime() {
        return currentTime;
    }
    
    public int getVesselCount() {
        return vesselCount;
    }
    
    public int getInWorldCount() {
        return inWorldCount;
    }
    public void setInWorldCount(int inWorldCount) {
        this.inWorldCount = inWorldCount;
    }
    
}

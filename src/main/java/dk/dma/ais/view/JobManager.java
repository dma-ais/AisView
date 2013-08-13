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

import static java.util.Objects.requireNonNull;
import jsr166e.ConcurrentHashMapV8;
import dk.dma.ais.store.AisStoreQueryResult;

/**
 * 
 * @author Kasper Nielsen
 */
public class JobManager {
    private final ConcurrentHashMapV8<String, Job> jobs = new ConcurrentHashMapV8<>();

    public void addJob(String key, AisStoreQueryResult queryResult) {
        jobs.put(requireNonNull(key), new Job(queryResult));
    }

    public void cleanup() {

    }

    public AisStoreQueryResult getResult(String key) {
        Job j = jobs.get(key);
        return j == null ? null : j.queryResult;
    }

    static class Job {
        final AisStoreQueryResult queryResult;

        Job(AisStoreQueryResult queryResult) {
            this.queryResult = requireNonNull(queryResult);
        }
    }

}

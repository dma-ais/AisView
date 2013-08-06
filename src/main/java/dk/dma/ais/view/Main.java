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

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Injector;

import dk.dma.ais.reader.AisReaderGroup;
import dk.dma.ais.reader.AisReaders;
import dk.dma.ais.store.AisStoreConnection;
import dk.dma.ais.view.tracker.TargetTrackerBackupService;
import dk.dma.commons.app.AbstractDaemon;

/**
 * 
 * @author Kasper Nielsen
 */
public class Main extends AbstractDaemon {
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger(Main.class);

    @Parameter(names = "-port", description = "The port to run AisView at")
    int port = 8090;

    @Parameter(names = "-backup", description = "The backup directory")
    File backup = new File("aisview-backup");

    @Parameter(names = "-databaseName", description = "The cassandra database to write data to")
    String cassandraDatabase = "aisdata";

    @Parameter(names = "-database", description = "A list of cassandra hosts that can store the data")
    List<String> cassandraSeeds = Arrays.asList("10.3.240.203");

    @Parameter(names = "-nodatabase", description = "Disables access to ais store")
    boolean disableAisStore;

    @Parameter(description = "A list of AIS sources (sourceName=host:port,host:port sourceName=host:port ...")
    List<String> aissources;

    /** {@inheritDoc} */
    @Override
    protected void runDaemon(Injector injector) throws Exception {

        // Setup the readers
        AisReaderGroup g = null;
        if (aissources != null && aissources.size() > 0) {
            g = AisReaders.createGroup("AisView", aissources);
            AisReaders.manageGroup(g);

        }
        // Setup AisStore
        AisStoreConnection con = null;
        if (!disableAisStore) {
            con = start(AisStoreConnection.create("aisdata", cassandraSeeds));
        }
        final AisViewer viewer = new AisViewer(g, con);

        // start the tracker if we get data
        if (g != null) {
            Files.createDirectories(backup.toPath());
            viewer.targetTracker.updateFrom(g);
            start(new TargetTrackerBackupService(viewer.targetTracker, backup.toPath()));
            start(new AbstractScheduledService() {
                protected Scheduler scheduler() {
                    return Scheduler.newFixedRateSchedule(1, 1, TimeUnit.MINUTES);
                }

                protected void runOneIteration() throws Exception {
                    viewer.targetTracker.clean();
                }
            });

            start(g.asService());
        }

        WebServer ws = new WebServer(port);
        ws.start(viewer);
        LOG.info("AisView started");
        ws.join();
    }

    public static void main(String[] args) throws Exception {
        new Main().execute(AisReaders.getDefaultSources());
    }
}

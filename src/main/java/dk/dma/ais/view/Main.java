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

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Injector;

import dk.dma.ais.reader.AisReaderGroup;
import dk.dma.ais.store.AisStore;
import dk.dma.ais.store.cassandra.CassandraAisStore;
import dk.dma.ais.store.cassandra.support.KeySpaceConnection;
import dk.dma.ais.view.tracker.TargetTrackerBackupService;
import dk.dma.commons.app.AbstractDaemon;

/**
 * 
 * @author Kasper Nielsen
 */
public class Main extends AbstractDaemon {

    @Parameter(names = "-port", description = "The port to run AisView at")
    int port = 8090;

    @Parameter(names = "-backup", description = "The backup directory")
    File backup = new File("aisview-backup");

    @Parameter(names = "-databaseName", description = "The cassandra database to write data to")
    String cassandraDatabase = "aisdata";

    @Parameter(names = "-database", description = "A list of cassandra hosts that can store the data")
    List<String> cassandraSeeds = Arrays.asList("localhost");

    @Parameter(names = "-nodatabase", description = "Disables access to ais store")
    boolean disableAisStore;

    @Parameter(description = "A list of AIS sources (sourceName=host:port,host:port sourceName=host:port ...")
    List<String> aissources;

    /** {@inheritDoc} */
    @Override
    protected void runDaemon(Injector injector) throws Exception {

        // Setup the readers
        AisReaderGroup g = AisReaderGroup.create(aissources);

        // Setup AisStore
        AisStore aisStore = null;
        if (!disableAisStore) {
            KeySpaceConnection con = start(KeySpaceConnection.connect("aisdata", cassandraSeeds));
            aisStore = new CassandraAisStore(con);
        }

        final AisViewer viewer = new AisViewer(g, aisStore);

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
        ws.join();
    }

    public static void main(String[] args) throws Exception {
        new Main().execute(AisReaderGroup.getDefaultSources());
    }
}

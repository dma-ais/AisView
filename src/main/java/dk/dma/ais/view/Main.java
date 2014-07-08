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
package dk.dma.ais.view;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.google.inject.Injector;

import dk.dma.ais.reader.AisReaderGroup;
import dk.dma.ais.reader.AisReaders;
import dk.dma.ais.store.job.JobManager;
import dk.dma.ais.tracker.TargetTracker;
import dk.dma.ais.tracker.TargetTrackerFileBackupService;
import dk.dma.ais.view.rest.WebServer;
import dk.dma.commons.app.AbstractDaemon;
import dk.dma.commons.web.rest.AbstractResource;
import dk.dma.db.cassandra.CassandraConnection;

/**
 * 
 * @author Kasper Nielsen
 * 
 * @deprecated in favor off  AisViewDaemon
 */
@Deprecated
public class Main extends AbstractDaemon {


    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger(Main.class);

    @Parameter(names = "-port", description = "The port to run AisView at")
    int port = 8090;

    @Parameter(names = "-backup", description = "The backup directory")
    File backup = new File("aisview-backup");

    @Parameter(names = "-databaseName", description = "The cassandra database to write data to")
    String cassandraDatabase = "aisdata";

    @Parameter(names = "-database", description = "A list of cassandra hosts that can store the data, list=empty -> AisStore disabled")
    List<String> cassandraSeeds = Collections.emptyList();

    @Parameter(description = "A list of AIS sources (sourceName=host:port,host:port sourceName=host:port ...")
    List<String> sources;

    /** {@inheritDoc} */
    @Override
    protected void runDaemon(Injector injector) throws Exception {

        final TargetTracker targetTracker = new TargetTracker();

        // Setup the readers
        AisReaderGroup g = AisReaders.createGroup("AisView", sources == null ? Collections.<String> emptyList()
                : sources);
        AisReaders.manageGroup(g);

        // A job manager that takes care of tracking ongoing jobs
        JobManager jobManager = new JobManager();

        // Setup the backup process
        Files.createDirectories(backup.toPath());
        start(new TargetTrackerFileBackupService(targetTracker, backup.toPath()));

        // We don't do any cleaning
        // start(new AbstractScheduledService() {
        // protected Scheduler scheduler() {
        // return Scheduler.newFixedRateSchedule(1, 1, TimeUnit.MINUTES);
        // }
        //
        // protected void runOneIteration() throws Exception {
        // targetTracker.clean();
        // }
        // });
        targetTracker.readFromStream(g.stream());
        
        start(g.asService());

        // Start Ais Store Connection
        CassandraConnection con = cassandraSeeds.isEmpty() ? null : start(CassandraConnection.create("aisdata",
                cassandraSeeds));

        WebServer ws = new WebServer(port);
        ws.getContext().setAttribute(AbstractResource.CONFIG,
                AbstractResource.create(g, con, targetTracker, jobManager));

        ws.start();
        LOG.info("AisView started");
        ws.join();
    }

    public static void main(String[] args) throws Exception {
        // args = AisReaders.getDefaultSources();
        //new Main().execute(args);
        throw new UnsupportedOperationException("Deppecated, see AisView/AisViewDaemon");
    }
}

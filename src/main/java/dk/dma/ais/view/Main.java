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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.google.inject.Injector;

import dk.dma.ais.reader.AisReaderGroup;
import dk.dma.ais.reader.AisReaders;
import dk.dma.ais.store.AisStoreConnection;
import dk.dma.ais.tracker.TargetTracker;
import dk.dma.commons.app.AbstractDaemon;
import dk.dma.commons.web.rest.AbstractResource;

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
    boolean disableAisStore = true;

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

        // Files.createDirectories(backup.toPath());
        // start(new TargetTrackerFileBackupService(targetTracker, backup.toPath()));

        // start(new AbstractScheduledService() {
        // protected Scheduler scheduler() {
        // return Scheduler.newFixedRateSchedule(1, 1, TimeUnit.MINUTES);
        // }
        //
        // protected void runOneIteration() throws Exception {
        // targetTracker.clean();
        // }
        // });
        targetTracker.readFrom(g.stream());
        start(g.asService());

        // Start Ais Store Connection
        AisStoreConnection con = disableAisStore ? null : start(AisStoreConnection.create("aisdata", cassandraSeeds));

        WebServer ws = new WebServer(port);
        ws.getContext().setAttribute(AbstractResource.CONFIG, AbstractResource.create(g, con, targetTracker));

        ws.start();
        LOG.info("AisView started");
        ws.join();
    }

    public static void main(String[] args) throws Exception {
        args = AisReaders.getDefaultSources();
        new Main().execute(args);
    }
}

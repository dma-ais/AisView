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
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import com.google.inject.Injector;

import dk.dma.ais.data.IPastTrack;
import dk.dma.ais.reader.AisReaderGroup;
import dk.dma.ais.reader.AisReaders;
import dk.dma.ais.store.job.JobManager;
import dk.dma.ais.tracker.TargetTracker;
import dk.dma.ais.tracker.TargetTrackerFileBackupService;
import dk.dma.ais.view.common.util.CacheManager;
import dk.dma.ais.view.rest.WebServer;
import dk.dma.commons.app.AbstractDaemon;
import dk.dma.commons.web.rest.AbstractResource;
import dk.dma.db.cassandra.CassandraConnection;

/**
 * AIS viewer daemon
 */
public class AisViewDaemon extends AbstractDaemon {
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger(AisViewDaemon.class);

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

    @Override
    protected void runDaemon(Injector injector) throws Exception {
        final TargetTracker targetTracker = new TargetTracker();
        final CacheManager cacheManager = new CacheManager();

        // Setup the readers
        AisReaderGroup g = AisReaders.createGroup("AisView",
                sources == null ? Collections.<String> emptyList() : sources);
        AisReaders.manageGroup(g);

        // A job manager that takes care of tracking ongoing jobs
        JobManager jobManager = new JobManager();

        // Setup the backup process
        // Files.createDirectories(backup);
        Files.createParentDirs(backup);

        start(new TargetTrackerFileBackupService(targetTracker, backup.toPath()));

        // start tracking
        targetTracker.readFromStream(g.stream());

        start(g.asService());

        // Start Ais Store Connection
        CassandraConnection con = cassandraSeeds.isEmpty() ? null
                : start(CassandraConnection.create("aisdata", cassandraSeeds));

        WebServer ws = new WebServer(port);
        ws.getContext().setAttribute(
                AbstractResource.CONFIG,
                AbstractResource.create(g, con, targetTracker, cacheManager,
                        jobManager));

        ws.start();
        LOG.info("AisView started");
        ws.join();
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOG.error(
                        "Uncaught exception in thread "
                                + t.getClass().getCanonicalName() + ": "
                                + e.getMessage(), e);
                System.exit(-1);
            }
        });
        new AisViewDaemon().execute(args);
    }

}

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
package dk.dma.ais.view.rest.resources;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import dk.dma.ais.packet.AisPacketStream;
import dk.dma.ais.store.AisStoreConnection;
import dk.dma.ais.tracker.TargetTracker;
import dk.dma.ais.view.AisViewer;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractViewerResource {

    public static final String VIEWER_ATTRIBUTE = "viewer";

    @Context
    ServletConfig servletConfig;

    final AisStoreConnection getStore() {
        return viewer().getAisStore();
    }

    final TargetTracker getTracker() {
        return viewer().getTracker();
    }

    final AisPacketStream newLiveStream() {
        return viewer().newLiveStream();
    }

    private AisViewer viewer() {
        return (AisViewer) servletConfig.getServletContext().getAttribute(VIEWER_ATTRIBUTE);
    }
}

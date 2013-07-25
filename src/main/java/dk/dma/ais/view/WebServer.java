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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.view.rest.resources.AbstractViewerResource;
import dk.dma.ais.view.rest.resources.StreamResource;

/**
 * 
 * @author Kasper Nielsen
 */
public class WebServer {
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger(StreamResource.class);

    final Server server;
    static final boolean secure = false;

    public WebServer(int port) {
        server = new Server(port);
    }

    public void start(AisViewer viewer) throws Exception {
        ((ServerConnector) server.getConnectors()[0]).setReuseAddress(true);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setAttribute(AbstractViewerResource.VIEWER_ATTRIBUTE, viewer);

        ServletHolder sho = new ServletHolder(new ServletContainer());
        sho.setClassName("org.glassfish.jersey.servlet.ServletContainer");
        sho.setInitParameter("jersey.config.server.provider.packages", "dk.dma.ais.view.rest.resources");
        // This flag is set to disable internal buffering in jersey.
        // this is mainly done to avoid delays from when people request something. To the first output is delivered
        sho.setInitParameter(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, "-1");
        context.addServlet(sho, "/*");

        Handler hd = context;
        if (secure) {
            SecurityHandler sh = getSecurityHandler();
            sh.setHandler(context);
            hd = sh;
        }

        HandlerWrapper hw = new HandlerWrapper() {

            /** {@inheritDoc} */
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                long start = System.nanoTime();
                LOG.info("Received connection from " + request.getRemoteHost() + " (" + request.getRemoteAddr() + ":"
                        + request.getRemotePort() + ") request = " + request.getRequestURI() + "?"
                        + request.getQueryString());
                super.handle(target, baseRequest, request, response);
                LOG.info("Connection closed from " + request.getRemoteHost() + " (" + request.getRemoteAddr() + ":"
                        + request.getRemotePort() + ") request = " + request.getRequestURI() + "?"
                        + request.getQueryString() + ", Duration = " + (System.nanoTime() - start) / 1000000 + " ms");
            }
        };
        hw.setHandler(hd);
        server.setHandler(hw);
        server.start();
    }

    private SecurityHandler getSecurityHandler() {

        // add authentication
        Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, "user");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[] { "user", "admin" });

        // map the security constraint to the root path.
        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        // create the security handler, set the authentication to Basic
        // and assign the realm.
        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName("AisViewer");
        csh.addConstraintMapping(cm);

        // set the login service
        csh.setLoginService(getHashLoginService());

        return csh;
    }

    private HashLoginService getHashLoginService() {

        // create the login service, assign the realm and read the user credentials
        // from the file /tmp/realm.properties.
        HashLoginService hls = new HashLoginService();
        hls.putUser("kasper", Credential.getCredential("dav"), new String[] { "user" });
        hls.setName("AisViewer");
        return hls;
    }

    public void join() throws InterruptedException {
        server.join();
    }
}

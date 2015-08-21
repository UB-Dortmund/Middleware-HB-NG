/*
    The MIT License (MIT)

    Copyright (c) 2015, Hans-Georg Becker, http://orcid.org/0000-0003-0432-294X

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
 */
package de.tu_dortmund.ub.hb_ng.middleware;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.*;
import java.util.Properties;

/**
 * Middleware HB-NG
 *
 * @author Dipl.-Math. Hans-Georg Becker, M.L.I.S. (UB Dortmund)
 * @version 2015-08-21
 *
 */
public class MiddlewareHbNg {

    private static final String     CONFIG_PROPERTIES_FILE_NAME = "config.properties";
    private static final String     CONF_FOLDER_NAME            = "conf";
    public static final  String     UTF_8                       = "UTF-8";
    private static Properties config                      = new Properties();

    private static Logger logger = Logger.getLogger(MiddlewareHbNg.class.getName());

    public static void main(String[] args) throws Exception {

        // config
        String conffile = CONF_FOLDER_NAME + File.separatorChar + CONFIG_PROPERTIES_FILE_NAME;

        // read program parameters
        if (args.length > 0) {

            for (final String arg : args) {

                logger.info("arg = " + arg);

                if (arg.startsWith("-conf=")) {

                    conffile = arg.split("=")[1];
                }
            }
        }

        // Init properties
        try {

            try (InputStream inputStream = new FileInputStream(conffile)) {

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {

                    config.load(reader);
                }
            }
        }
        catch (IOException e) {

            logger.error("something went wrong", e);
            logger.error(String.format("FATAL ERROR: Could not read '%s'!", conffile));
        }

        // init logger
        PropertyConfigurator.configure(config.getProperty(HBNGStatics.SERVICE_LOG4J_CONF_IDENTIFIER));

        final String serviceName = config.getProperty(HBNGStatics.SERVICE_NAME_IDENTIFIER);

        logger.info(String.format("[%s] Starting '" + MiddlewareHbNg.class.getName() + "' ...", serviceName));
        logger.info(String.format("[%s] conf-file = %s", serviceName, conffile));

        final String log4jConfFile = config.getProperty(HBNGStatics.SERVICE_LOG4J_CONF_IDENTIFIER);

        logger.info(String.format("[%s] log4j-conf-file = %s", serviceName, log4jConfFile));

        // Server
        Server server = new Server(Integer.parseInt(config.getProperty(HBNGStatics.SERVICE_PORT_IDENTIFIER)));

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath(config.getProperty(HBNGStatics.SERVICE_CONTEXTPATH_IDENTIFIER));
        server.setHandler(context);

        // Endpoints
        // - home
        ServletHolder holderHome = new ServletHolder("static-home", DefaultServlet.class);
        holderHome.setInitParameter("resourceBase", config.getProperty(HBNGStatics.ENDPOINT_HOME_CONTENT_IDENTIFIER));
        context.addServlet(holderHome, "/*");

        // - resource
        context.addServlet(new ServletHolder(new MiddlewareHbNgEndpoint(conffile)), config.getProperty(HBNGStatics.ENDPOINT_CONTEXTPATH_IDENTIFIER));

        // Start Server
        server.start();
        server.join();
    }
}

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

import de.tu_dortmund.ub.hb_ng.middleware.preprocessing.PreprocessingInterface;
import de.tu_dortmund.ub.util.impl.Lookup;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Middleware HB-NG Endpoint
 *
 * @author Dipl.-Math. Hans-Georg Becker, M.L.I.S. (UB Dortmund)
 * @version 2015-08-21
 *
 */
public class MiddlewareHbNgEndpoint extends HttpServlet {

    public static final  String     UTF_8                       = "UTF-8";
    private static Properties config                      = new Properties();

    private static Logger logger = Logger.getLogger(MiddlewareHbNgEndpoint.class.getName());

    public MiddlewareHbNgEndpoint(String conffile) {

        // Init properties
        try {

            try (InputStream inputStream = new FileInputStream(conffile)) {

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {

                    this.config.load(reader);
                }
            }
        }
        catch (IOException e) {

            this.logger.error("something went wrong", e);
            this.logger.error(String.format("FATAL ERROR: Could not read '%s'!", conffile));
        }

        // init logger
        PropertyConfigurator.configure(this.config.getProperty(HBNGStatics.SERVICE_LOG4J_CONF_IDENTIFIER));

        final String serviceName = this.config.getProperty(HBNGStatics.SERVICE_NAME_IDENTIFIER);

        logger.info(String.format("[%s] Starting '" + MiddlewareHbNgEndpoint.class.getName() + "' ...", serviceName));
        logger.info(String.format("[%s] conf-file = %s", serviceName, conffile));

        final String log4jConfFile = this.config.getProperty(HBNGStatics.SERVICE_LOG4J_CONF_IDENTIFIER);

        logger.info(String.format("[%s] log4j-conf-file = %s", serviceName, log4jConfFile));
    }

    public void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setHeader("Access-Control-Allow-Methods", this.config.getProperty(HBNGStatics.CORS_ACCESS_CONTROL_ALLOW_METHODS_IDENTIFIER));
        response.addHeader("Access-Control-Allow-Headers", this.config.getProperty(HBNGStatics.CORS_ACCESS_CONTROL_ALLOW_HEADERS_IDENTIFIER));
        response.setHeader("Access-Control-Allow-Origin", this.config.getProperty(HBNGStatics.CORS_ACCESS_CONTROL_ALLOW_ORIGIN_IDENTIFIER));
        response.setHeader("Accept", this.config.getProperty(HBNGStatics.CORS_ACCEPT_IDENTIFIER));

        response.getWriter().println();
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        // CORS ORIGIN RESPONSE HEADER
        httpServletResponse.setHeader("Access-Control-Allow-Origin", config.getProperty(HBNGStatics.CORS_ACCESS_CONTROL_ALLOW_ORIGIN_IDENTIFIER));

        String authorization = "";
        String contenttype = "";

        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while ( headerNames.hasMoreElements() ) {

            String headerNameKey = headerNames.nextElement();
            this.logger.debug("headerNameKey = " + headerNameKey + " / headerNameValue = " + httpServletRequest.getHeader(headerNameKey));

            if (headerNameKey.equals("Authorization")) {
                authorization = httpServletRequest.getHeader( headerNameKey );
            }
            if (headerNameKey.equals("Content-Type")) {
                contenttype = httpServletRequest.getHeader(headerNameKey);
            }
        }

        this.logger.info("contenttype = " + contenttype);

        try {

            // TODO validate Content-Type

            String data = httpServletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

            if (data == null || data.equals("")) {

                this.logger.error(HttpServletResponse.SC_NO_CONTENT + " - No Content");
                httpServletResponse.sendError(HttpServletResponse.SC_NO_CONTENT, "No Content");
            }
            else {

                String postableData = null;

                // TODO bind interface Preprocessing
                if (Lookup.lookupAll(PreprocessingInterface.class).size() > 0) {

                    PreprocessingInterface preprocessingInterface = Lookup.lookup(PreprocessingInterface.class);
                    // init Authorization Service
                    preprocessingInterface.init(this.config);

                    postableData = preprocessingInterface.process(data);
                }
                else {

                    // TODO correct error handling
                    this.logger.error("[" + this.config.getProperty("service.name") + "] " + HttpServletResponse.SC_INTERNAL_SERVER_ERROR + ": " + "Authorization Interface not implemented!");
                }

                if (postableData != null) {

                    // TODO if successful then POST as application/sparql-update to LinkedDataPlatform
                    String sparql_url = this.config.getProperty("ldp.sparql-endpoint");

                    // HTTP Request
                    int timeout = Integer.parseInt(this.config.getProperty("ldp.timeout"));

                    RequestConfig defaultRequestConfig = RequestConfig.custom()
                            .setSocketTimeout(timeout)
                            .setConnectTimeout(timeout)
                            .setConnectionRequestTimeout(timeout)
                            .build();

                    CloseableHttpClient httpclient = HttpClients.custom()
                            .setDefaultRequestConfig(defaultRequestConfig)
                            .build();

                    try {

                        HttpPost httpPost = new HttpPost(sparql_url);
                        httpPost.addHeader("Content-Type", "application/sparql-update");
                        httpPost.addHeader("Authorization", this.config.getProperty("ldp.authorization"));
                        httpPost.setEntity(new StringEntity(postableData));

                        CloseableHttpResponse httpResponse = null;

                        long start = System.nanoTime();
                        try {

                            httpResponse = httpclient.execute(httpPost);
                        }
                        catch (ConnectTimeoutException | SocketTimeoutException e) {

                            this.logger.info("[" + this.getClass().getName() + "] " + e.getClass().getName() + ": " + e.getMessage());
                            httpResponse = httpclient.execute(httpPost);
                        }
                        long elapsed = System.nanoTime() - start;
                        this.logger.info("[" + this.getClass().getName() + "] LDP request - " + (elapsed / 1000.0 / 1000.0 / 1000.0) + " s");

                        try {

                            int statusCode = httpResponse.getStatusLine().getStatusCode();
                            HttpEntity httpEntity = httpResponse.getEntity();

                            // TODO
                            httpServletResponse.setStatus(statusCode);
                            httpServletResponse.getWriter().println(httpResponse.getStatusLine().getReasonPhrase());

                            EntityUtils.consume(httpEntity);

                        }
                        finally {
                            httpResponse.close();
                        }
                    }
                    finally {

                        httpclient.close();
                    }
                }
            }
        }
        catch (Exception e) {

            this.logger.error("something went wrong", e);
            httpServletResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "something went wrong");
        }
    }
}

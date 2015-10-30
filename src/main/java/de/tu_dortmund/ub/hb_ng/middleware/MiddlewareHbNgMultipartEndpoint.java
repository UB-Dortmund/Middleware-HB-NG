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
import javax.servlet.http.Part;
import java.io.*;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by Hans-Georg on 29.10.2015.
 */
public class MiddlewareHbNgMultipartEndpoint extends HttpServlet {

    public static final String UTF_8 = "UTF-8";

    private Properties config = new Properties();
    private Logger logger = Logger.getLogger(MiddlewareHbNgEndpoint.class.getName());

    public MiddlewareHbNgMultipartEndpoint(String conffile) {

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

        // work on headers
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

        // work on parameters
        String id = "";

        if (httpServletRequest.getParameter("uuid") != null && !httpServletRequest.getParameter("uuid").equals("")) {

            id = httpServletRequest.getParameter("uuid");
        }
        else {

            id = UUID.randomUUID().toString();
        }
        this.logger.info("ID: " + id);

        // constructs path of the directory to save uploaded file
        String savePath = this.config.getProperty("multipart.path.uploadedFiles") + File.separator + id;

        // creates the save directory if it does not exists
        File fileSaveDir = new File(this.config.getProperty("multipart.path.base") + savePath);
        if (!fileSaveDir.exists()) {

            this.logger.info("Verzeichnis existiert nicht! Lege '" + savePath + "' an.");
            fileSaveDir.mkdir();
        }
        else {

            this.logger.info("Verzeichnis '" + savePath + "' existiert!");
        }

        // work on request parts
        try {

            String modsData = null;
            String modsDataContentType = null;
            String cslData = null;
            String cslDataContentType = null;
            HashMap<String, HashMap<String, String>> swordTasks = new HashMap<String, HashMap<String, String>>();

            boolean isTask = true;

            for (Part part : httpServletRequest.getParts()) {

                // new publications as file upload
                if (part.getName().startsWith("file-")) {

                    String fileName = extractFileName(part);
                    this.logger.info("partname: " + part.getName() + "; filename: " + fileName);

                    part.write(savePath + File.separator + fileName);

                    // TODO create new Ticket

                    isTask = false;
                }
                // Uploads
                else if (part.getName().startsWith("resource-") && part.getName().endsWith("-file")) {

                    String fileName = extractFileName(part);
                    this.logger.info("partname: " + part.getName() + "; filename: " + fileName);

                    part.write(savePath + File.separator + fileName);

                    // add to SWORD-Task
                    if (!swordTasks.containsKey(part.getName().split("-file")[0])) {

                        HashMap<String, String> swordTask = new HashMap<String, String>();

                        swordTask.put("file", fileName);

                        swordTasks.put(part.getName().split("-file")[0], swordTask);
                    }
                    else {

                        swordTasks.get(part.getName().split("-file")[0]).put("file", fileName);
                    }
                }
                else if (part.getName().startsWith("resource-") && part.getName().endsWith("-type")) {

                    String value = new BufferedReader(new InputStreamReader(part.getInputStream())).lines().collect(Collectors.joining(System.lineSeparator()));

                    this.logger.info(part.getName() + ": " + value);

                    // add to SWORD-Task
                    if (!swordTasks.containsKey(part.getName().split("-type")[0])) {

                        HashMap<String, String> swordTask = new HashMap<String, String>();

                        swordTask.put("type", value);

                        swordTasks.put(part.getName().split("-type")[0], swordTask);
                    }
                    else {

                        swordTasks.get(part.getName().split("-type")[0]).put("type", value);
                    }
                }
                else if (part.getName().startsWith("resource-") && part.getName().endsWith("-note")) {

                    String value = new BufferedReader(new InputStreamReader(part.getInputStream())).lines().collect(Collectors.joining(System.lineSeparator()));

                    this.logger.info(part.getName() + ": " + value);

                    // add to SWORD-Task
                    if (!swordTasks.containsKey(part.getName().split("-note")[0])) {

                        HashMap<String, String> swordTask = new HashMap<String, String>();

                        swordTask.put("note", value);

                        swordTasks.put(part.getName().split("-note")[0], swordTask);
                    }
                    else {

                        swordTasks.get(part.getName().split("-note")[0]).put("note", value);
                    }
                }
                else if (part.getName().startsWith("dataset-mods")) {

                    modsData = new BufferedReader(new InputStreamReader(part.getInputStream())).lines().collect(Collectors.joining(System.lineSeparator()));
                    modsDataContentType = part.getContentType();

                    this.logger.debug(part.getName() + ": " + modsData);
                }
                else if (part.getName().startsWith("dataset-csl")) {

                    cslData = new BufferedReader(new InputStreamReader(part.getInputStream())).lines().collect(Collectors.joining(System.lineSeparator()));
                    cslDataContentType = part.getContentType();

                    this.logger.debug(part.getName() + ": " + cslData);
                }
                else {

                    this.logger.info(part.getName() + ": " + new BufferedReader(new InputStreamReader(part.getInputStream())).lines().collect(Collectors.joining(System.lineSeparator())));
                }
            }

            String data = "";

            try {

                data = httpServletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                this.logger.info("RequestBody is not empty!");
                this.logger.info("data: " + data);

            } catch (Exception e) {

                this.logger.info("RequestBody is empty!");
            }

            // validate existence of modsData and its content type > SC_NO_CONTENT
            if (modsData == null || modsData.equals("")) {

                this.logger.error(HttpServletResponse.SC_NO_CONTENT + " - No Content");
                httpServletResponse.sendError(HttpServletResponse.SC_NO_CONTENT, "No Content");
            }
            else {

                // PROCESSING
                if (isTask) {

                    // Migrate and POST to LDP
                    String postableData = null;

                    // bind interface Preprocessing
                    if (Lookup.lookupAll(PreprocessingInterface.class).size() > 0) {

                        PreprocessingInterface preprocessingInterface = Lookup.lookup(PreprocessingInterface.class);
                        // init Authorization Service
                        preprocessingInterface.init(this.config);

                        postableData = preprocessingInterface.process(modsData);
                    }
                    else {

                        // correct error handling
                        this.logger.error("[" + this.config.getProperty("service.name") + "] " + HttpServletResponse.SC_INTERNAL_SERVER_ERROR + ": " + "Authorization Interface not implemented!");
                    }

                    // POST MODS data to LDP
                    boolean isModsDataPosted = false;

                    if (postableData != null) {

                        // POST as application/sparql-update to LinkedDataPlatform
                        String sparql_url = this.config.getProperty("ldp.sparql-endpoint");

                        // HTTP Request
                        int timeout = Integer.parseInt(this.config.getProperty("ldp.timeout"));

                        RequestConfig defaultRequestConfig = RequestConfig.custom()
                                .setSocketTimeout(timeout)
                                .setConnectTimeout(timeout)
                                .setConnectionRequestTimeout(timeout)
                                .build();

                        try (CloseableHttpClient httpclient = HttpClients.custom()
                                .setDefaultRequestConfig(defaultRequestConfig)
                                .build()) {

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

                                if (statusCode == 200 || statusCode == 201) {

                                    isModsDataPosted = true;
                                }
                                else {
                                    this.logger.error("POST to LDP failed! - " + statusCode + ": " + httpResponse.getStatusLine().getReasonPhrase());
                                }

                                EntityUtils.consume(httpEntity);

                            } finally {
                                httpResponse.close();
                            }
                        }
                    }

                    if (isModsDataPosted) {

                        // TODO work on CSL data

                        // TODO work on SWORD-Tasks
                        // Interface Repository with implementation for SWORD using https://github.com/swordapp/JavaClient2.0
                        if (swordTasks.size() > 0) {

                            for (String taskid : swordTasks.keySet()) {

                                System.out.println("TASK: '" + taskid + "'");
                                System.out.println("type: " + swordTasks.get(taskid).get("type"));
                                System.out.println("file: " + this.config.getProperty("multipart.path.base") + savePath + File.separator + swordTasks.get(taskid).get("file"));
                                System.out.println("note: " + swordTasks.get(taskid).get("note"));

                                // TODO Was passiert mit den MODS- und ggf. CSL-Daten, wenn das hier schiefgeht?
                                // 1. Möglichkeit: Lösche MODS und CSL und geben 500 zurück
                                // 2. Möglichkeit: Kopiere die Dateien in ein "manuell bearbeiten"-Verzeichnis und sende 201
                                // Mein Favorit: 2. Möglichkeit
                            }
                        }
                    }

                    // last but not least: delete 'uploadFiles'-subfolder
                    deleteTree(new File(this.config.getProperty("multipart.path.base") + savePath));
                }
            }

            // response
            httpServletResponse.setStatus(HttpServletResponse.SC_CREATED);
            httpServletResponse.getWriter().println("");
        }
        catch (Exception e) {

            this.logger.error("something went wrong", e);
            httpServletResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "something went wrong");
        }
    }

    protected void doPut(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        // CORS ORIGIN RESPONSE HEADER
        httpServletResponse.setHeader("Access-Control-Allow-Origin", config.getProperty(HBNGStatics.CORS_ACCESS_CONTROL_ALLOW_ORIGIN_IDENTIFIER));

        // work on headers
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

        // work on parameters
        String id = "";

        if (httpServletRequest.getParameter("uuid") != null && !httpServletRequest.getParameter("uuid").equals("")) {

            id = httpServletRequest.getParameter("uuid");
        }
        else {

            id = UUID.randomUUID().toString();
        }
        this.logger.info("ID: " + id);

        // constructs path of the directory to save uploaded file
        String savePath = this.config.getProperty("multipart.path.uploadedFiles") + File.separator + id;

        // creates the save directory if it does not exists
        File fileSaveDir = new File(this.config.getProperty("multipart.path.base") + savePath);
        if (!fileSaveDir.exists()) {

            this.logger.info("Verzeichnis existiert nicht! Lege '" + savePath + "' an.");
            fileSaveDir.mkdir();
        }
        else {

            this.logger.info("Verzeichnis '" + savePath + "' existiert!");
        }

        // work on request parts
        try {

            String modsData = null;
            String modsDataContentType = null;
            String cslData = null;
            String cslDataContentType = null;
            HashMap<String, HashMap<String,String>> swordTasks = new HashMap<String, HashMap<String,String>>();

            for (Part part : httpServletRequest.getParts()) {

                // Uploads
                if (part.getName().startsWith("resource-") && part.getName().endsWith("-file")) {

                    String fileName = extractFileName(part);
                    this.logger.info("partname: " + part.getName() + "; filename: " + fileName);

                    part.write(savePath + File.separator + fileName);

                    // add to SWORD-Task
                    if (!swordTasks.containsKey(part.getName().split("-file")[0])) {

                        HashMap<String, String> swordTask = new HashMap<String,String>();

                        swordTask.put("file", fileName);

                        swordTasks.put(part.getName().split("-file")[0], swordTask);
                    }
                    else {

                        swordTasks.get(part.getName().split("-file")[0]).put("file", fileName);
                    }
                }
                else if (part.getName().startsWith("resource-") && part.getName().endsWith("-type")) {

                    String value = new BufferedReader(new InputStreamReader(part.getInputStream())).lines().collect(Collectors.joining(System.lineSeparator()));

                    this.logger.info(part.getName() + ": " + value);

                    // add to SWORD-Task
                    if (!swordTasks.containsKey(part.getName().split("-type")[0])) {

                        HashMap<String, String> swordTask = new HashMap<String,String>();

                        swordTask.put("type", value);

                        swordTasks.put(part.getName().split("-type")[0], swordTask);
                    }
                    else {

                        swordTasks.get(part.getName().split("-type")[0]).put("type", value);
                    }
                }
                else if (part.getName().startsWith("resource-") && part.getName().endsWith("-note")) {

                    String value = new BufferedReader(new InputStreamReader(part.getInputStream())).lines().collect(Collectors.joining(System.lineSeparator()));

                    this.logger.info(part.getName() + ": " + value);

                    // add to SWORD-Task
                    if (!swordTasks.containsKey(part.getName().split("-note")[0])) {

                        HashMap<String, String> swordTask = new HashMap<String,String>();

                        swordTask.put("note", value);

                        swordTasks.put(part.getName().split("-note")[0], swordTask);
                    }
                    else {

                        swordTasks.get(part.getName().split("-note")[0]).put("note", value);
                    }
                }
                // MODS data
                else if (part.getName().startsWith("dataset-mods")) {

                    modsData = new BufferedReader(new InputStreamReader(part.getInputStream())).lines().collect(Collectors.joining(System.lineSeparator()));
                    modsDataContentType = part.getContentType();

                    this.logger.debug(part.getName() + ": " + modsData);
                }
                // CSL data
                else if (part.getName().startsWith("dataset-csl")) {

                    cslData = new BufferedReader(new InputStreamReader(part.getInputStream())).lines().collect(Collectors.joining(System.lineSeparator()));
                    cslDataContentType = part.getContentType();

                    this.logger.debug(part.getName() + ": " + cslData);
                }
                else {

                    this.logger.info(part.getName() + ": " + new BufferedReader(new InputStreamReader(part.getInputStream())).lines().collect(Collectors.joining(System.lineSeparator())));
                }
            }

            String data = "";

            try {

                data = httpServletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            }
            catch (Exception e) {

                this.logger.info("RequestBody is empty!");
            }

            this.logger.info("data: " + data);

            // validate existence of modsData and its content type > SC_NO_CONTENT
            if (modsData == null || modsData.equals("")) {

                this.logger.error(HttpServletResponse.SC_NO_CONTENT + " - No Content");
                httpServletResponse.sendError(HttpServletResponse.SC_NO_CONTENT, "No Content");
            }
            else {

                // PROCESSING

                // Migrate and POST to LDP
                String postableData = null;

                // bind interface Preprocessing
                if (Lookup.lookupAll(PreprocessingInterface.class).size() > 0) {

                    PreprocessingInterface preprocessingInterface = Lookup.lookup(PreprocessingInterface.class);
                    // init Authorization Service
                    preprocessingInterface.init(this.config);

                    postableData = preprocessingInterface.process(modsData);
                } else {

                    // correct error handling
                    this.logger.error("[" + this.config.getProperty("service.name") + "] " + HttpServletResponse.SC_INTERNAL_SERVER_ERROR + ": " + "Authorization Interface not implemented!");
                }

                boolean isModsDataPosted = false;

                if (postableData != null) {

                    // if successful then POST as application/sparql-update to LinkedDataPlatform
                    String sparql_url = this.config.getProperty("ldp.sparql-endpoint");

                    // HTTP Request
                    int timeout = Integer.parseInt(this.config.getProperty("ldp.timeout"));

                    RequestConfig defaultRequestConfig = RequestConfig.custom()
                            .setSocketTimeout(timeout)
                            .setConnectTimeout(timeout)
                            .setConnectionRequestTimeout(timeout)
                            .build();

                    // TODO DELETE in LDP
                    boolean isModsDataDeleted = false;

                    // POST to LDP
                    if (isModsDataDeleted) {

                        try (CloseableHttpClient httpclient = HttpClients.custom()
                                .setDefaultRequestConfig(defaultRequestConfig)
                                .build()) {

                            HttpPost httpPost = new HttpPost(sparql_url);
                            httpPost.addHeader("Content-Type", "application/sparql-update");
                            httpPost.addHeader("Authorization", this.config.getProperty("ldp.authorization"));
                            httpPost.setEntity(new StringEntity(postableData));

                            CloseableHttpResponse httpResponse = null;

                            long start = System.nanoTime();
                            try {

                                httpResponse = httpclient.execute(httpPost);
                            } catch (ConnectTimeoutException | SocketTimeoutException e) {

                                this.logger.info("[" + this.getClass().getName() + "] " + e.getClass().getName() + ": " + e.getMessage());
                                httpResponse = httpclient.execute(httpPost);
                            }
                            long elapsed = System.nanoTime() - start;
                            this.logger.info("[" + this.getClass().getName() + "] LDP request - " + (elapsed / 1000.0 / 1000.0 / 1000.0) + " s");

                            try {

                                int statusCode = httpResponse.getStatusLine().getStatusCode();
                                HttpEntity httpEntity = httpResponse.getEntity();

                                if (statusCode == 200 || statusCode == 201) {

                                    isModsDataPosted = true;
                                } else {
                                    this.logger.error("POST to LDP failed! - " + statusCode + ": " + httpResponse.getStatusLine().getReasonPhrase());
                                }

                                EntityUtils.consume(httpEntity);

                            } finally {
                                httpResponse.close();
                            }
                        }
                    }
                }

                if (isModsDataPosted) {

                    // TODO POST updates to Repository REST API

                    // TODO work on CSL data

                    // TODO work on SWORD-Tasks
                    // Interface Repository with implementation for SWORD using https://github.com/swordapp/JavaClient2.0
                    if (swordTasks.size() > 0) {

                        for (String taskid : swordTasks.keySet()) {

                            System.out.println("TASK: '" + taskid + "'");
                            System.out.println("type: " + swordTasks.get(taskid).get("type"));
                            System.out.println("file: " + this.config.getProperty("multipart.path.base") + savePath + File.separator + swordTasks.get(taskid).get("file"));
                            System.out.println("note: " + swordTasks.get(taskid).get("note"));

                            // TODO Was passiert mit den MODS- und ggf. CSL-Daten, wenn das hier schiefgeht?
                            // 1. Möglichkeit: Lösche MODS und CSL und geben 500 zurück
                            // 2. Möglichkeit: Kopiere die Dateien in ein "manuell bearbeiten"-Verzeichnis und sende 201
                            // Mein Favorit: 2. Möglichkeit
                        }
                    }
                }

                // last but not least: delete 'uploadFiles'-subfolder
                deleteTree(new File(this.config.getProperty("multipart.path.base") + savePath));
            }

            // response
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            httpServletResponse.getWriter().println("");
        }
        catch (Exception e) {

            this.logger.error("something went wrong", e);
            httpServletResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "something went wrong");
        }
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String id = "";

        if (request.getParameter("uri") != null && !request.getParameter("uri").equals("")) {

            id = request.getParameter("uri");
        }
        this.logger.info("URI: " + id);

        // Processing

        // TODO DELETE in LDP

        // response
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.getWriter().println("Resource '<" + id + ">' deleted!");
    }

    /**
     * Extracts file name from HTTP header content-disposition
     */
    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length()-1);
            }
        }
        return "";
    }

    /**
     * Deletes a given directory recursively
     * @param path
     */
    public void deleteTree( File path ) {

        for ( File file : path.listFiles() ) {

            if ( file.isDirectory() )
                deleteTree( file );
            else if ( ! file.delete() )
                this.logger.error( file + " could not be deleted!" );
        }

        if ( ! path.delete())
            this.logger.error( path + " could not be deleted!" );
    }

}

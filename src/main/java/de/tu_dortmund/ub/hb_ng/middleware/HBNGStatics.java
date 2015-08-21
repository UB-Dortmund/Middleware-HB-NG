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

/**
 * Middleware HB-NG Statics
 *
 * @author Dipl.-Math. Hans-Georg Becker, M.L.I.S. (UB Dortmund)
 * @version 2015-08-21
 *
 */
public class HBNGStatics {

    // property names
    public static final String SERVICE_NAME_IDENTIFIER                      = "service.name";
    public static final String SERVICE_LOG4J_CONF_IDENTIFIER                = "service.log4j-conf";
    public static final String SERVICE_PORT_IDENTIFIER                      = "service.port";
    public static final String SERVICE_CONTEXTPATH_IDENTIFIER               = "service.contextpath";
    public static final String SERVICE_LANGUAGE_DEFAULT_IDENTIFIER          = "service.language.default";
    public static final String SERVICE_IPRANGE_TU_IDENTIFIER                = "service.iprange.tu";
    public static final String SERVICE_IPRANGE_TU_EXCEPTIONS_IDENTIFIER     = "service.iprange.tu.exceptions";
    public static final String SERVICE_IPRANGE_UB_IDENTIFIER                = "service.iprange.ub";
    public static final String SERVICE_IPRANGE_UB_EXCEPTIONS_IDENTIFIER     = "service.iprange.ub.exceptions";

    public static final String ENDPOINT_HOME_CONTENT_IDENTIFIER         = "endpoint.home.content";
    public static final String ENDPOINT_CONTEXTPATH_IDENTIFIER          = "endpoint.contextpath";

    public static final String CORS_ACCESS_CONTROL_ALLOW_METHODS_IDENTIFIER = "cors.access-control-allow-methods";
    public static final String CORS_ACCESS_CONTROL_ALLOW_HEADERS_IDENTIFIER = "cors.access-control-allow-headers";
    public static final String CORS_ACCESS_CONTROL_ALLOW_ORIGIN_IDENTIFIER  = "cors.access-control-allow-origin";
    public static final String CORS_ACCEPT_IDENTIFIER                       = "cors.accept";

    // values
    public static final String FORMAT_HTML = "html";
    public static final String FORMAT_XML = "xml";
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_RDF_XML = "rdf.xml";
    public static final String FORMAT_TURTLE = "rdf.ttl";

    public static final String APPLICATION_XML_MIMETYPE = "application/xml";
    public static final String APPLICATION_JSON_MIMETYPE = "application/json";
    public static final String APPLICATION_RDF_XML_MIMETYPE = "application/rdf+xml";
    public static final String APPLICATION_JSON_LD_MIMETYPE = "application/ld+json";
    public static final String TEXT_TURTLE_MIMETYPE = "text/turtle";
    public static final String TEXT_HTML_MIMETYPE = "text/html";
}

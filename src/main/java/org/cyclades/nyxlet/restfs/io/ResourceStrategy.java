/*******************************************************************************
 * Copyright (c) 2012, THE BOARD OF TRUSTEES OF THE LELAND STANFORD JUNIOR UNIVERSITY
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *    Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *    Neither the name of the STANFORD UNIVERSITY nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.cyclades.nyxlet.restfs.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cyclades.engine.NyxletSession;
import org.cyclades.engine.nyxlet.templates.stroma.actionhandler.ActionHandler;
import org.cyclades.io.ResourceRequestUtils;
import org.cyclades.nyxlet.restfs.Main;
import org.json.JSONArray;
import org.json.JSONObject;
import org.cyclades.engine.util.TransactionIdentifier;
import org.w3c.dom.Node;

public abstract class ResourceStrategy {

    public abstract void writeResource (String path, byte[] data) throws Exception;
    public abstract void writeResourceEnhancedXML (String path, Node node) throws Exception;
    public abstract void writeResource (String path, InputStream is) throws Exception;
    public abstract void writeResource (String path, String inputURI) throws Exception;
    public abstract void readResource (String path, OutputStream os) throws Exception;
    public abstract byte[] readResource (String path) throws Exception;
    public abstract boolean removeResource (String path, boolean isDirectory) throws Exception;
    public abstract InputStream getResourceInputStream (String source) throws Exception;
    public abstract void transferDirectoryResource(String absoluteSourceURI, String absoluteDestURI, Boolean httpDestination) throws Exception;
    public abstract void transferCompressedDirectoryResource(String absoluteSourceURI, String absoluteDestURI, boolean unzipRemote) throws Exception;
    public abstract void decompressDirectoryResource (String absoluteSourceURI, String absoluteDestURI, boolean removeCompDir) throws Exception;

    public void init (JSONObject initData) throws Exception {
        documentRoot = initData.getString("documentRoot");
    }

    public void destroy () throws Exception {
        // No Op
    }

    public void transferResource(String absoluteDestURI, String absoluteSourceURI) throws Exception {
        try {
            if (absoluteDestURI.toLowerCase().startsWith("http")) {
                transferResourceHTTP(absoluteSourceURI, absoluteDestURI);
            } else {
                transferResourceLocal(absoluteSourceURI, absoluteDestURI);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void transferResource(String absoluteDestURI, byte[] data) throws Exception {
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(data);
            if (absoluteDestURI.toLowerCase().startsWith("http")) {
                transferResourceHTTP(is, absoluteDestURI);
            } else {
                transferResourceLocal(is, absoluteDestURI);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }

    public void transferResourceLocal (String source, String dest) throws Exception {
        InputStream is = null;
        try {
            is = getResourceInputStream(source);
            transferResourceLocal(is, dest);
        } catch (Exception e) {
            throw e;
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }

    public void transferResourceHTTP (String source, String dest, String ... strings) throws Exception {
        InputStream is = null;
        try {
            if (strings.length % 2 != 0) throw new Exception("ResourceStrategy.transferResourceHTTP: Must supply key/value pairs as parameters");
            StringBuilder sb = new StringBuilder(dest).append("?raw-response");
            for (int i = 0; i < strings.length;) {
                sb.append("&").append(strings[i++]).append("=").append(strings[i++]);
            }
            is = getResourceInputStream(source);
            transferResourceHTTP(is, sb.toString());
        } catch (Exception e) {
            throw e;
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }

    public void transferResourceLocal (InputStream is, String dest) throws Exception {
        writeResource(dest, is);
    }

    public void transferResourceHTTP (InputStream is, String dest) throws Exception {
        InputStream tempIS = null;
        try {
            tempIS = ResourceRequestUtils.getInputStreamHTTP (dest, "POST", is, HEADER_PROPERTIES, -1, -1);
        } finally {
            try { tempIS.close(); } catch (Exception e) {}
        }
    }

    /**
     * Extract the resource path from a Nyxlet request. The algorithm is as follows:
     *
     * 1.) If the parameter uri exists, return its value
     * 2.) If valid URI input exists, return its value as a URI string
     * 3.) Return null if there is no valid path source detected
     *
     * @param nyxletSession     The request NyxletSession
     * @param baseParameters    The request base parameters
     * @param offset            The offset from which to begin creating the path. If from a typical service invocation, this
     *                          would be 1, if from a RRD request, this may be 0.
     * @return resource path
     * @throws Exception
     */
    public static String getRequestResourcePath (NyxletSession nyxletSession, Map<String, List<String>> baseParameters, int offset) throws Exception {
        if (baseParameters.containsKey(URI_FIELD)) return cleanURI(baseParameters.get(URI_FIELD).get(0));
        String webServiceRequest = nyxletSession.getRequestPathInfo();
        if (webServiceRequest != null && webServiceRequest.length() > 1) {
            String[] URIParts = webServiceRequest.split("/");
            if (URIParts.length > 1 && offset < URIParts.length - 1) {
                StringBuilder sb = new StringBuilder();
                int loopCount = 0;
                for (int i = offset + 1; i < URIParts.length; i++) {
                    if (loopCount++ > 0) sb.append("/");
                    sb.append(URIParts[i]);
                }
                return sb.toString();
            }
        }
        return null;
    }

    private static String cleanURI (String URI) {
        if (URI.length() > 0 && URI.charAt(0) == '/') return URI.substring(1);
        return URI;
    }

    public String getDocumentRoot () {
        return documentRoot;
    }

    public static ResourceStrategy getResourceStrategy (ActionHandler handler, Map<String, List<String>> baseParameters) throws Exception {
        return ((Main)handler.getParentNyxlet()).getResourceStrategy(baseParameters);
    }

    public static String getDocumentTempPath (String path) {
        return new StringBuilder(path).append(".").append(transactionIdentifier.getTransactionID()).toString();
    }

    public static Map<String, ResourceStrategy> generateResourceStrategies (JSONArray resourceStrategyArray) throws Exception {
        final String eLabel = "ResourceStrategy.generateResourceStrategies: ";
        Map<String, ResourceStrategy> resourceStrategyMap = new HashMap<String, ResourceStrategy>();
        try {
            ResourceStrategy resourceStrategy;
            JSONObject resourceDefinition;
            for (int i = 0; i < resourceStrategyArray.length(); i++) {
                resourceDefinition = resourceStrategyArray.getJSONObject(i);
                resourceStrategy = ResourceStrategyEnum.valueOf(resourceDefinition.getString("type").toUpperCase()).newResourceStrategy();
                resourceStrategy.init(resourceDefinition.getJSONObject("initData"));
                resourceStrategyMap.put(resourceDefinition.getString("name"), resourceStrategy);
            }
            return resourceStrategyMap;
        } catch (Exception e) {
            for (Map.Entry <String, ResourceStrategy> resEntry : resourceStrategyMap.entrySet()) {
                try {
                    resEntry.getValue().destroy();
                } catch (Exception ex) {}
            }
            throw new Exception(eLabel + e);
        }
    }

    private static TransactionIdentifier transactionIdentifier = null;
    protected static Map<String, String> HEADER_PROPERTIES = new HashMap<String, String>();

    static {
        try { transactionIdentifier = new TransactionIdentifier("restfs-"); } catch (Exception e) {}
        HEADER_PROPERTIES.put("content-type", "");
    }

    protected String documentRoot;
    public final static String URI_FIELD            = "uri";
    public final static String MAP_CHANNEL_OBJECT   = "binary";
    public final static String PAYLOAD_PARAMETER    = "payload";
    public final static String SOURCE_PARAMETER     = "source";

}

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
package org.cyclades.nyxlet.restfs.actionhandler;

import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamWriter;
import org.cyclades.annotations.AHandler;
import org.cyclades.engine.NyxletSession;
import org.cyclades.engine.nyxlet.templates.stroma.STROMANyxlet;
import org.cyclades.engine.nyxlet.templates.stroma.actionhandler.ActionHandler;
import org.cyclades.engine.nyxlet.templates.xstroma.OrchestrationTypeEnum;
import org.cyclades.engine.stroma.STROMAResponseWriter;
import org.cyclades.nyxlet.restfs.io.ResourceStrategy;
import org.cyclades.nyxlet.restfs.io.StatusCodeEnum;

/**
 * ActionHandler to handle removing a resource
 */
@AHandler({"DELETE", "delete"})
public class RemoveHandler extends ActionHandler {

    public RemoveHandler (STROMANyxlet parentNyxlet) {
        super(parentNyxlet);
    }

    @Override
    public void handle (NyxletSession nyxletSession, Map<String, List<String>> baseParameters, STROMAResponseWriter stromaResponseWriter) throws Exception {
        final String eLabel = "RemoveHandler.handle: ";
        try {
            ResourceStrategy resourceStrategy = ResourceStrategy.getResourceStrategy(this, baseParameters);
            StatusCodeEnum statusCodeEnum = StatusCodeEnum.SUCCESS;
            String message;
            String resourceURI;
            if (nyxletSession.getOrchestrationTypeEnum().equals(OrchestrationTypeEnum.NONE)) {
                resourceURI = ResourceStrategy.getRequestResourcePath(nyxletSession, baseParameters, 1);
            } else {
                resourceURI = (baseParameters.containsKey(ResourceStrategy.URI_FIELD)) ? baseParameters.get(ResourceStrategy.URI_FIELD).get(0) : null;
            }
            if (resourceURI == null) {
                statusCodeEnum = StatusCodeEnum.REQUEST_FORMAT_ERROR;
                message = "Cannot detect path source (uri)";
            } else {
                String absoluteResourceURI = new StringBuilder(resourceStrategy.getDocumentRoot()).append("/").append(resourceURI).toString();
                try {
                    if (!resourceStrategy.removeResource(absoluteResourceURI, baseParameters.containsKey(DIRECTORY_MODE))) {
                        statusCodeEnum = StatusCodeEnum.RESOURCE_NOT_FOUND_ERROR;
                        getParentNyxlet().logError(eLabel + "Resource does not exist: " + absoluteResourceURI);
                    }
                } catch (Exception e) {
                    statusCodeEnum = StatusCodeEnum.INTERNAL_ERROR;
                    getParentNyxlet().logStackTrace(e);
                }
                message = resourceURI;
            }
            if (nyxletSession.rawResponseRequested()) nyxletSession.getHttpServletResponse().setStatus(Integer.parseInt(statusCodeEnum.getCode()));
            XMLStreamWriter streamWriter = stromaResponseWriter.getXMLStreamWriter();
            streamWriter.writeStartElement("entity");
            streamWriter.writeAttribute("status-code", statusCodeEnum.getCode());
            streamWriter.writeAttribute("message", message);
            streamWriter.writeEndElement();
        } catch (Exception e) {
            getParentNyxlet().logStackTrace(e);
            handleException(nyxletSession, stromaResponseWriter, eLabel, e);
        } finally {
            stromaResponseWriter.done();
        }
    }

    public static final String DIRECTORY_MODE = "dir";

}

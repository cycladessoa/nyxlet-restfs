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
package org.cyclades.nyxlet.restfs.io.fs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.cyclades.io.FileUtils;
import org.cyclades.io.ResourceRequestUtils;
import org.cyclades.io.Zip;
import org.cyclades.nyxlet.restfs.io.ResourceStrategy;
import org.cyclades.xml.parser.api.XMLGeneratedObject;
import com.google.common.io.ByteStreams;
import org.w3c.dom.Node;

public class FSResourceStrategy extends ResourceStrategy {

    @Override
    public void writeResource (String path, byte[] data) throws Exception {
        final String eLabel = "FSResourceStrategy.writeResource: ";
        try {
            String tempPath = getDocumentTempPath(path);
            FileUtils.verifyFileOutputDirectory(tempPath);
            FileUtils.writeToFile(data, tempPath);
            renameResource(tempPath, path);
        } catch (Exception e) {
            throw new Exception(eLabel + e);
        }
    }

    @Override
    public void writeResourceEnhancedXML (String path, Node node) throws Exception {
        final String eLabel = "FSResourceStrategy.writeResourceEnhancedXML: ";
        OutputStream os = null;
        try {
            String tempPath = getDocumentTempPath(path);
            FileUtils.verifyFileOutputDirectory(tempPath);
            os = new FileOutputStream(new File(tempPath));
            XMLGeneratedObject.writeToStreamResult(new DOMSource(node), new StreamResult(os), true);
            os.flush();
            os.close(); // Make sure it is closed before renaming
            renameResource(tempPath, path);
        } catch (Exception e) {
            throw new Exception(eLabel + e);
        } finally {
            try { os.close(); } catch (Exception e) {}
        }
    }

    @Override
    public void writeResource (String path, InputStream is) throws Exception {
        final String eLabel = "FSResourceStrategy.writeResource: ";
        OutputStream os = null;
        try {
            String tempPath = getDocumentTempPath(path);
            FileUtils.verifyFileOutputDirectory(tempPath);
            os = new FileOutputStream(new File(tempPath));
            ByteStreams.copy(is, os);
            os.close(); // Make sure it is closed before renaming
            renameResource(tempPath, path);
        } catch (Exception e) {
            throw new Exception(eLabel + e);
        } finally {
            try { os.close(); } catch (Exception e) {}
        }
    }

    @Override
    public void writeResource (String path, String inputURI) throws Exception {
        final String eLabel = "FSResourceStrategy.writeResource: ";
        InputStream is = null;
        try {
            is = ResourceRequestUtils.getInputStream(inputURI, null);
            writeResource(path, is);
        } catch (Exception e) {
            throw new Exception(eLabel + e);
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }

    @Override
    public void readResource (String path, OutputStream os) throws Exception {
        final String eLabel = "FSResourceStrategy.readResource: ";
        InputStream is = null;
        try {
            is = ResourceRequestUtils.getInputStream(path, null);
            ByteStreams.copy(is, os);
        } catch (Exception e) {
            throw new Exception(eLabel + e);
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }

    @Override
    public byte[] readResource (String path) throws Exception {
        return ResourceRequestUtils.getData(path, null);
    }

    @Override
    public boolean removeResource (String path, boolean isDirectory) throws Exception {
        File resourceToDelete = new File(path);
        if (!resourceToDelete.exists()) {
            return false;
        } else {
            if (isDirectory) {
                if (!FileUtils.deleteDirectoryContents(path, true)) throw new Exception("Failed to remove resource: " + path);
                if (!resourceToDelete.delete()) throw new Exception("Failed to remove resource: " + path);
            } else {
                if (!resourceToDelete.isFile()) throw new Exception("Resource is not a file: " + path);
                if (!resourceToDelete.delete()) throw new Exception("Failed to remove resource: " + path);
            }
            return true;
        }
    }

    @Override
    public InputStream getResourceInputStream (String source) throws Exception {
        return ResourceRequestUtils.getInputStream(source, null);
    }

    @Override
    public void transferDirectoryResource (String absoluteSourceURI, String absoluteDestURI, Boolean httpDestination) throws Exception {
        InputStream is = null;
        Boolean httpDestinationLocal = (httpDestination == null) ? absoluteDestURI.toLowerCase().startsWith("http") : httpDestination;
        File directory = new File(absoluteSourceURI);
        if (!directory.exists()) {
            throw new IOException("Directory does not exist: " + absoluteSourceURI);
        }
        if (!directory.isDirectory()) {
            throw new IOException("Not a directory: " + absoluteSourceURI);
        }
        File[] children = directory.listFiles();
        for (int i = 0; i < children.length; i++) {
            if (children[i].isFile()) {
                if (httpDestinationLocal) {
                    transferResourceHTTP(new StringBuilder(absoluteSourceURI).append("/").append(children[i].getName()).toString(),
                                         new StringBuilder(absoluteDestURI).append("/").append(children[i].getName()).toString());
                } else {
                    transferResourceLocal(new StringBuilder(absoluteSourceURI).append("/").append(children[i].getName()).toString(),
                            new StringBuilder(absoluteDestURI).append("/").append(children[i].getName()).toString());
                }
            } else {
                transferDirectoryResource(new StringBuilder(absoluteSourceURI).append("/").append(children[i].getName()).toString(),
                                          new StringBuilder(absoluteDestURI).append("/").append(children[i].getName()).toString(),
                                          httpDestinationLocal);
            }
        }
    }

    @Override
    public void transferCompressedDirectoryResource (String absoluteSourceURI, String absoluteDestURI, boolean unzipDest) throws Exception {
        InputStream is = null;
        Boolean httpDestination = absoluteDestURI.toLowerCase().startsWith("http");
        File directory = new File(absoluteSourceURI);
        if (!directory.exists()) {
            throw new IOException("Directory does not exist: " + absoluteSourceURI);
        }
        if (!directory.isDirectory()) {
            throw new IOException("Not a directory: " + absoluteSourceURI);
        }
        if (httpDestination) {
            String tempPath = getDocumentTempPath(absoluteSourceURI);
            Zip.zipDirectory(absoluteSourceURI, tempPath);
            if (unzipDest) {
                transferResourceHTTP(tempPath, absoluteDestURI, "action", "postcompressed");
            } else {
                transferResourceHTTP(tempPath, absoluteDestURI);
            }
            removeResource(tempPath, false);
        } else {
            String tempPath = getDocumentTempPath(absoluteDestURI);
            FileUtils.verifyFileOutputDirectory(tempPath);
            Zip.zipDirectory(absoluteSourceURI, tempPath);
            if (unzipDest) {
                Zip.unzipDirectory(tempPath, absoluteDestURI);
                removeResource(tempPath, false);
            } else {
                renameResource (tempPath, absoluteDestURI);
            }
        }
    }

    @Override
    public void decompressDirectoryResource (String absoluteSourceURI, String absoluteDestURI, boolean removeCompDir) throws Exception {
        File zipFile = new File(absoluteSourceURI);
        if (!zipFile.exists()) {
            throw new IOException("Directory does not exist: " + absoluteSourceURI);
        }
        if (!zipFile.isFile()) {
            throw new IOException("Not a file: " + absoluteSourceURI);
        }
        Zip.unzipDirectory(absoluteSourceURI, absoluteDestURI);
        if (removeCompDir) removeResource(absoluteSourceURI, false);
    }

    private static void renameResource (String from, String to) throws Exception {
        if (!new File(from).renameTo(new File(to))) throw new Exception("Failed to rename file [" + from + "] to [" + to + "]");
    }

}

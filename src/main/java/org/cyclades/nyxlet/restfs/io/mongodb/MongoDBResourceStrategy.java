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
package org.cyclades.nyxlet.restfs.io.mongodb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.cyclades.engine.util.MapHelper;
import org.cyclades.io.ResourceRequestUtils;
import org.cyclades.nyxlet.restfs.io.ResourceStrategy;
import org.cyclades.xml.parser.api.XMLGeneratedObject;
import org.w3c.dom.Node;
import com.google.common.io.ByteStreams;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.DBObject;
import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.ReadPreference;
import org.json.JSONObject;
import org.json.JSONArray;

public class MongoDBResourceStrategy extends ResourceStrategy {

    /**
     * configData is optional. If it is not present, the driver will default to a basic local instance
     * of MongoDB. If it is present, the following field and formatting rules apply:
     *
     * user:            optional, omit to bypass authentication. If this field exists, "password" must also exist.
     * password:        only necessary when the "user" field is provided.
     * mongoOptions:    optional, omit for default MongoOptions (See MongoDB JavaDoc re MongoOptions for details).
     * servers:         required, at least one entry is required.
     * readPreference:  optional, omit to default to "primary". Must be one of either "secondary" or "primary".
     * database:        optional, omit to default to "restfs".
     * gridFSBucket:    optional, omit to default to "restfs".
     *
     * Example property configData JSON:
     *
     * {"documentRoot":"/tmp","user":"guest","password":"guest","mongoOptions":{},"servers":[{"host":"127.0.0.1","port":"27017"}],"readPreference":"primary","database":"restfs","gridFSBucket":"restfs"}
     *
     */
    @Override
    public void init (JSONObject initData) throws Exception {
        try {
            super.init(initData);
            if (initData == null) {
                mongo = new Mongo();
                DB db = mongo.getDB(DEFAULT_DATABASE);
                gridFS = new GridFS(db, DEFAULT_GRIDFS_BUCKET);
            } else {
                mongo = new Mongo(getServerList(initData), getMongoOptions(initData));
                mongo.setReadPreference(getReadPreference(initData));
                DB db = mongo.getDB(getDatabase(initData));
                authenticate(db, initData);
                gridFS = new GridFS(db, getGridFSBucket(initData));
            }
        } catch (Exception e) {
            try { mongo.close(); } catch (Exception ex) {};
            mongo = null;
            throw e;
        }
    }

    private List<ServerAddress> getServerList (JSONObject jsonObject) throws Exception {
        List<ServerAddress> serverList = new ArrayList<ServerAddress>();
        JSONArray serverJSONObjects = jsonObject.getJSONArray(SERVERS);
        for (int i = 0; i < serverJSONObjects.length(); i++) {
            serverList.add(new ServerAddress(serverJSONObjects.getJSONObject(i).getString(HOST), Integer.parseInt(serverJSONObjects.getJSONObject(i).getString(PORT))));
        }
        return serverList;
    }

    private MongoOptions getMongoOptions (JSONObject jsonObject) throws Exception {
        return new MongoOptionsBuilder().build((jsonObject.has(MONGODB_OPTIONS)) ? MapHelper.mapFromMeta(jsonObject.getJSONObject(MONGODB_OPTIONS)) : new HashMap<String, String>());
    }

    private ReadPreference getReadPreference (JSONObject jsonObject) throws Exception {
        ReadPreference readPreference = ReadPreference.PRIMARY;
        if (jsonObject.has(READ_PREFERENCE)) {
            String readPreferenceString = jsonObject.getString(READ_PREFERENCE);
            if (readPreferenceString.equalsIgnoreCase(SECONDARY)) {
                readPreference = ReadPreference.SECONDARY;
            } else if (!readPreferenceString.equalsIgnoreCase(PRIMARY)) {
                throw new Exception("Invalid read preference: " + readPreference);
            }
        }
        return readPreference;
    }

    private void authenticate (DB db, JSONObject jsonObject) throws Exception {
        if (jsonObject.has(USER)) {
            if (!db.authenticate(jsonObject.getString(USER), jsonObject.getString(PASSWORD).toCharArray())) throw new Exception("MongoDB Authentication Failed!");
        }
    }

    private String getDatabase (JSONObject jsonObject) throws Exception {
        if (jsonObject.has(DATABASE)) return jsonObject.getString(DATABASE);
        return DEFAULT_DATABASE;
    }

    private String getGridFSBucket (JSONObject jsonObject) throws Exception {
        if (jsonObject.has(GRIDFS_BUCKET)) return jsonObject.getString(GRIDFS_BUCKET);
        return DEFAULT_GRIDFS_BUCKET;
    }

    @Override
    public void destroy () throws Exception {
        mongo.close();
        gridFS = null;
        mongo = null;
    }

    public void writeResource (String path, byte[] data) throws Exception {
        writeResource(path, new ByteArrayInputStream(data));
    }

    @Override
    public void writeResourceEnhancedXML (String path, Node node) throws Exception {
        final String eLabel = "MongoDBResourceStrategy.writeResourceEnhancedXML: ";
        OutputStream os = null;
        try {
            os = getResourceOutputStream(path);
            XMLGeneratedObject.writeToStreamResult(new DOMSource(node), new StreamResult(os), true);
        } catch (Exception e) {
            throw new Exception(eLabel + e);
        } finally {
            try { os.close(); } catch (Exception e) {}
        }
    }

    @Override
    public void writeResource (String path, InputStream is) throws Exception {
        final String eLabel = "MongoDBResourceStrategy.writeResource: ";
        OutputStream os = null;
        try {
            os = getResourceOutputStream(path);
            ByteStreams.copy(is, os);
        } catch (Exception e) {
            throw new Exception(eLabel + e);
        } finally {
            try { os.close(); } catch (Exception e) {}
        }
    }

    @Override
    public void writeResource (String path, String inputURI) throws Exception {
        final String eLabel = "MongoDBResourceStrategy.writeResource: ";
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

    private OutputStream getResourceOutputStream (String path) throws Exception {
        DBObject key = getDBObject(path, false);
        gridFS.remove(path);
        GridFSInputFile file = gridFS.createFile();
        file.setFilename(path);
        file.setMetaData(key);
        return file.getOutputStream();
    }

    @Override
    public void readResource(String path, OutputStream os) throws Exception {
        final String eLabel = "MongoDBResourceStrategy.writeResource: ";
        try {
            List<GridFSDBFile> files = gridFS.find(path);
            if (files.size() != 1) throw new Exception("Invalid result count: " + path + " count: " + files.size());
            files.get(0).writeTo(os);
        } catch (Exception e) {
            throw new Exception(eLabel + e);
        }
    }

    @Override
    public byte[] readResource(String path) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        readResource(path, os);
        return os.toByteArray();
    }

    @Override
    public boolean removeResource(String path, boolean isDirectory) throws Exception {
        if (isDirectory) {
            gridFS.remove(getDBObject(path, true));
        } else {
            gridFS.remove(path);
        }
        return true;
    }

    @Override
    public void transferDirectoryResource(String absoluteSourceURI, String absoluteDestURI, Boolean httpDestination) throws Exception {
        throw new UnsupportedOperationException("MongoDBResourceStrategy: Not implemented yet!!!");
    }

    @Override
    public void transferCompressedDirectoryResource(String absoluteSourceURI, String absoluteDestURI, boolean unzipRemote) throws Exception {
        throw new UnsupportedOperationException("MongoDBResourceStrategy: Not implemented yet!!!");
    }

    @Override
    public void decompressDirectoryResource (String absoluteSourceURI, String absoluteDestURI, boolean removeCompDir) throws Exception {
        throw new UnsupportedOperationException("MongoDBResourceStrategy: Not implemented yet!!!");
    }

    @Override
    public InputStream getResourceInputStream (String source) throws Exception {
        final String eLabel = "MongoDBResourceStrategy.writeResource: ";
        try {
            List<GridFSDBFile> files = gridFS.find(source);
            if (files.size() != 1) throw new Exception("Invalid result count: " + source + " count: " + files.size());
            return files.get(0).getInputStream();
        } catch (Exception e) {
            throw new Exception(eLabel + e);
        }
    }

    private DBObject getDBObject (String URI, boolean query) throws Exception {
        List<String> URIParts = getURIParts(URI);
        if (URIParts.size() < 1) throw new Exception("At least one URI part required");
        BasicDBObject dbObject = new BasicDBObject();
        for (int i = 0; i < URIParts.size(); i++) {
            dbObject.put(new StringBuilder((query) ? "metadata." : "").append("urip").append(i).toString(), URIParts.get(i));
        }
        return dbObject;
    }

    private static List<String> getURIParts (String path) {
        ArrayList<String> partsList = new ArrayList<String>();
        String[] URIParts = path.split("/");
        for (String part : URIParts) {
            if (part.isEmpty()) continue;
            partsList.add(part);
        }
        return partsList;
    }

    private Mongo mongo;
    private GridFS gridFS;
    public static final String MONGODB_CONFIG           = "mongodbConfig";
    public static final String MONGODB_OPTIONS          = "mongoOptions";
    public static final String SERVERS                  = "servers";
    public static final String HOST                     = "host";
    public static final String PORT                     = "port";
    public static final String READ_PREFERENCE          = "readPreference";
    public static final String PRIMARY                  = "primary";
    public static final String SECONDARY                = "secondary";
    public static final String USER                     = "user";
    public static final String PASSWORD                 = "password";
    public static final String DATABASE                 = "database";
    public static final String GRIDFS_BUCKET            = "gridFSBucket";
    public static final String DEFAULT_DATABASE         = "restfs";
    public static final String DEFAULT_GRIDFS_BUCKET    = "restfs";

}
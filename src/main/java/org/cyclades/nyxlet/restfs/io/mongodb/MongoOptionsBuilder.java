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

import java.util.Map;
import com.mongodb.MongoOptions;

/**
 * Simple class to enable flexible population of a MongoOptions object from meta. This enables setting the fields of a
 * MongoOptions by passing in a Map<String, String> representing the settings. The keys can be mapped as necessary, or
 * simply left as is to default to the key names matching the field names of a MongoOptions, exactly.
 *
 * Example scenario: One may wish to set the "autoConnectRetry" value of a MongoOptions Object by using a key/value pair of "autoConnectRetryKey"/"true",
 * or may want to create a custom mapping for the same functionality like "mongo_autoConnectRetryKey"/"true".
 *
 * For the most part, the default will work, where no keys need to be re-mapped and the "build (Map<String, String> initializationMap)" can
 * simply be used. Custom mappings would be useful if somehow the default key names conflict with other keys used for other purposes in the
 * meta.
 */
public class MongoOptionsBuilder {

    public MongoOptions build (Map<String, String> initializationMap) {
        return build(initializationMap, "");
    }

    /**
     * Build a MongoOptions Object using the given Map, and appending the given prefix for key access to that Map
     *
     * @param initializationMap Values to populate the MongoOptions from
     * @param prefix            Prefix to add to current keys so they match the keys of the Map passed in
     * @return MongoOptions
     */
    public MongoOptions build (Map<String, String> initializationMap, String prefix) {
        MongoOptions options = new MongoOptions();
        if (initializationMap.containsKey(prefix + autoConnectRetryKey)) options.autoConnectRetry = Boolean.parseBoolean(initializationMap.get(prefix + autoConnectRetryKey));
        if (initializationMap.containsKey(prefix + connectionsPerHostKey)) options.connectionsPerHost = Integer.parseInt(initializationMap.get(prefix + connectionsPerHostKey));
        if (initializationMap.containsKey(prefix + connectTimeoutKey)) options.connectTimeout = Integer.parseInt(initializationMap.get(prefix + connectTimeoutKey));
        //if (initializationMap.containsKey(prefix + dbEncoderFactoryKey)) dbEncoderFactoryKey = ???(initializationMap.get(prefix + dbEncoderFactoryKey));
        //if (initializationMap.containsKey(prefix + dbEncoderFactoryKey)) dbEncoderFactoryKey = ???(initializationMap.get(prefix + dbEncoderFactoryKey));
        if (initializationMap.containsKey(prefix + descriptionKey)) options.description = initializationMap.get(prefix + descriptionKey);
        if (initializationMap.containsKey(prefix + fsyncKey)) options.fsync = Boolean.parseBoolean(initializationMap.get(prefix + fsyncKey));
        if (initializationMap.containsKey(prefix + jKey)) options.j = Boolean.parseBoolean(initializationMap.get(prefix + jKey));
        if (initializationMap.containsKey(prefix + maxAutoConnectRetryTimeKey)) options.maxAutoConnectRetryTime = Long.parseLong(initializationMap.get(prefix + maxAutoConnectRetryTimeKey));
        if (initializationMap.containsKey(prefix + maxWaitTimeKey)) options.maxWaitTime = Integer.parseInt(initializationMap.get(prefix + maxWaitTimeKey));
        if (initializationMap.containsKey(prefix + safeKey)) options.safe = Boolean.parseBoolean(initializationMap.get(prefix + safeKey));
        //if (initializationMap.containsKey(prefix + socketFactoryKey)) options.socketFactory = ???(initializationMap.get(prefix + socketFactoryKey));
        if (initializationMap.containsKey(prefix + socketKeepAliveKey)) options.socketKeepAlive = Boolean.parseBoolean(initializationMap.get(prefix + socketKeepAliveKey));
        if (initializationMap.containsKey(prefix + socketTimeoutKey)) options.socketTimeout = Integer.parseInt(initializationMap.get(prefix + socketTimeoutKey));
        if (initializationMap.containsKey(prefix + threadsAllowedToBlockForConnectionMultiplierKey)) options.threadsAllowedToBlockForConnectionMultiplier = Integer.parseInt(initializationMap.get(prefix + threadsAllowedToBlockForConnectionMultiplierKey));
        if (initializationMap.containsKey(prefix + wKey)) options.w = Integer.parseInt(initializationMap.get(prefix + wKey));
        if (initializationMap.containsKey(prefix + wtimeoutKey)) options.wtimeout = Integer.parseInt(initializationMap.get(prefix + wtimeoutKey));
        return options;
    }

    public MongoOptionsBuilder setAutoConnectRetryKey(String autoConnectRetryKey) {
        this.autoConnectRetryKey = autoConnectRetryKey;
        return this;
    }
    public MongoOptionsBuilder setConnectionsPerHostKey(String connectionsPerHostKey) {
        this.connectionsPerHostKey = connectionsPerHostKey;
        return this;
    }
    public MongoOptionsBuilder setConnectTimeoutKey(String connectTimeoutKey) {
        this.connectTimeoutKey = connectTimeoutKey;
        return this;
    }
    public MongoOptionsBuilder setDbDecoderFactoryKey(String dbDecoderFactoryKey) {
        this.dbDecoderFactoryKey = dbDecoderFactoryKey;
        return this;
    }
    public MongoOptionsBuilder setDbEncoderFactoryKey(String dbEncoderFactoryKey) {
        this.dbEncoderFactoryKey = dbEncoderFactoryKey;
        return this;
    }
    public MongoOptionsBuilder setDescriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
        return this;
    }
    public MongoOptionsBuilder setFsyncKey(String fsyncKey) {
        this.fsyncKey = fsyncKey;
        return this;
    }
    public MongoOptionsBuilder setjKey(String jKey) {
        this.jKey = jKey;
        return this;
    }
    public MongoOptionsBuilder setMaxAutoConnectRetryTimeKey(String maxAutoConnectRetryTimeKey) {
        this.maxAutoConnectRetryTimeKey = maxAutoConnectRetryTimeKey;
        return this;
    }
    public MongoOptionsBuilder setMaxWaitTimeKey(String maxWaitTimeKey) {
        this.maxWaitTimeKey = maxWaitTimeKey;
        return this;
    }
    public MongoOptionsBuilder setSafeKey(String safeKey) {
        this.safeKey = safeKey;
        return this;
    }
    public MongoOptionsBuilder setSocketFactoryKey(String socketFactoryKey) {
        this.socketFactoryKey = socketFactoryKey;
        return this;
    }
    public MongoOptionsBuilder setSocketKeepAliveKey(String socketKeepAliveKey) {
        this.socketKeepAliveKey = socketKeepAliveKey;
        return this;
    }
    public MongoOptionsBuilder setSocketTimeoutKey(String socketTimeoutKey) {
        this.socketTimeoutKey = socketTimeoutKey;
        return this;
    }
    public MongoOptionsBuilder setThreadsAllowedToBlockForConnectionMultiplierKey(
            String threadsAllowedToBlockForConnectionMultiplierKey) {
        this.threadsAllowedToBlockForConnectionMultiplierKey = threadsAllowedToBlockForConnectionMultiplierKey;
        return this;
    }
    public MongoOptionsBuilder setwKey(String wKey) {
        this.wKey = wKey;
        return this;
    }
    public MongoOptionsBuilder setWtimeoutKey(String wtimeoutKey) {
        this.wtimeoutKey = wtimeoutKey;
        return this;
    }

    private String autoConnectRetryKey                              = "autoConnectRetry";
    private String connectionsPerHostKey                            = "connectionsPerHost";
    private String connectTimeoutKey                                = "connectTimeout";
    private String dbDecoderFactoryKey                              = "dbDecoderFactory";
    private String dbEncoderFactoryKey                              = "dbEncoderFactory";
    private String descriptionKey                                   = "description";
    private String fsyncKey                                         = "fsync";
    private String jKey                                             = "j";
    private String maxAutoConnectRetryTimeKey                       = "maxAutoConnectRetryTime";
    private String maxWaitTimeKey                                   = "maxWaitTime";
    private String safeKey                                          = "safe";
    private String socketFactoryKey                                 = "socketFactory";
    private String socketKeepAliveKey                               = "socketKeepAlive";
    private String socketTimeoutKey                                 = "socketTimeout";
    private String threadsAllowedToBlockForConnectionMultiplierKey  = "threadsAllowedToBlockForConnectionMultiplier";
    private String wKey                                             = "w";
    private String wtimeoutKey                                      = "wtimeout";

}

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
package org.cyclades.nyxlet.restfs;

import java.util.List;
import java.util.Map;
import org.cyclades.annotations.Nyxlet;
import org.cyclades.engine.exception.CycladesException;
import org.cyclades.engine.nyxlet.templates.stroma.STROMANyxlet;
import org.cyclades.nyxlet.restfs.io.ResourceStrategy;
import org.json.JSONArray;

@Nyxlet
public class Main extends STROMANyxlet {

    public Main () throws Exception {
        super();
    }

    @Override
    public void init () throws CycladesException {
          final String eLabel = "Main.init: ";
          try {
              super.init();
              defaultResourceStrategy = getExternalProperties().getPropertyOrError("defaultResourceStrategy");
              resourceStrategies = ResourceStrategy.generateResourceStrategies(new JSONArray(getExternalProperties().getPropertyOrError("resourceStrategies")));
              for (String name : resourceStrategies.keySet()) logInfo("ResourceStrategy loaded: " + name);
          } catch (Exception e) {
              throw new CycladesException(eLabel + e);
          }
    }

    @Override
    public void destroy () throws CycladesException {
        for (Map.Entry <String, ResourceStrategy> resEntry : resourceStrategies.entrySet()) {
            try {
                resEntry.getValue().destroy();
            } catch (Exception ex) {}
        }
    }

    @Override
    public boolean isHealthy () throws CycladesException {
        if (super.isHealthy()) {
            // Recovery condition
            //setActive(true);
            return true;
        } else {
            //logError("Deactiviating the service");
            //setActive(false);
            return false;
        }
    }

    public ResourceStrategy getResourceStrategy (Map<String, List<String>> baseParameters) throws Exception {
        String key = (baseParameters.containsKey(RESOURCE_STRATEGY_PARAMETER)) ? baseParameters.get(RESOURCE_STRATEGY_PARAMETER).get(0) : defaultResourceStrategy;
        if (!resourceStrategies.containsKey(key)) throw new Exception("Resource strategy not found: " + key);
        return resourceStrategies.get(key);
    }

    private Map<String, ResourceStrategy> resourceStrategies;
    private String defaultResourceStrategy;
    private static String RESOURCE_STRATEGY_PARAMETER = "strategy";

}

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at


   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.eventmodel;

import org.apache.poi.hssf.model.Model;

/**
 * ModelFactoryListener is registered with the 
 * ModelFactory.  It receives Models.
 * 
 * @author Andrew C. Oliver acoliver@apache.org
 */
public interface ModelFactoryListener
{
    /**
     * Process a model.  Called by the ModelFactory
     * @param model to be processed
     * @return abortable - currently ignored (may be implemented in the future)
     */
    public boolean process(Model model);
}

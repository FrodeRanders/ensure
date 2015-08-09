/*
 * Copyright (C) 2011-2014 Frode Randers
 * All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The research leading to the implementation of this software package
 * has received funding from the European Community´s Seventh Framework
 * Programme (FP7/2007-2013) under grant agreement n° 270000.
 *
 * Frode Randers was at the time of creation of this software module
 * employed as a doctoral student by Luleå University of Technology
 * and remains the copyright holder of this material due to the
 * Teachers Exemption expressed in Swedish law (LAU 1949:345)
 */
package eu.ensure.packproc.model;

import java.util.Collection;
import java.util.Map;

/**
 * Description of ProcessorContext:
 * <p>*
 * Created by Frode Randers at 2011-12-14 00:26
 */
public interface ProcessorContext {

    // Get name of entity that this context clings to
    String getContextName();

    // Methods that handle the 'current context' stack
    <E extends ProcessorContext> E push(E previous);
    ProcessorContext pop();

    Collection<ProcessorContext> getContextStack();

    // Methods that handle the information hierarchy
    void addChild(ProcessorContext child);
    Collection<ProcessorContext> getChildren();

    // Methods that handle collected information in a context
    Map<String, String> getCollectedValues();

    // Methods that handle information on a per-file basis
    void associate(String claimant, String path, String providedPath, Map<String, String> map);
    Collection<? extends AssociatedInformation> extractAssociatedInformation();
}

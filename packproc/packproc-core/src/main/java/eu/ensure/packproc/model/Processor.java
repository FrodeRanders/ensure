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

import eu.ensure.packproc.ProcessorManager;
import eu.ensure.packproc.internal.Action;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.axiom.om.OMElement;

/**
 * Some kind of _recursive_ processor operating on _structures_ (such as ZIP- or TAR-files,
 * the filesystem, etc.) and _files_ (which could themselves be ZIP- or TAR-files, etc.)
 */
public interface Processor {
    /**
     * Since all processors are dynamically loaded and instantiated (based upon the
     * "classpath:"-based namespace definition) we have to provide an empty default
     * constructor for the Processor implementations.
     * <p/>
     * Because of this, we depend on delayed initialization using this method.
     */
    void initialize(
            ProcessorManager manager,
            Map<String, String> attributes,
            String text,
            String alias,
            Stack<Processor> outerProcessors
    );

    /**
     * A processor will (certainly) contain sub-processors, defined as actions. Typically
     * the PackageProcessor (that knows how to treat TAR-structures) will be
     * created with sub-processors (actions) that knows how to treat individual
     * files within that TAR-structure.
     * <p/>
     * A contrived example could be an Information Package with an embedded Information
     * Package, in which case the sub-processor (action) would be an PackageProcessor.
     */
    void define(List<Action> actions);
    List<Action> getActions();

    /**
     * If the configuration does not define sub-processors (actions) but still holds
     * XML elements, we will assume that these elements are part of the configuration
     * of the processor itself. We will pass on the (parent) element containing all
     * the configuration elements.
     */
    void setConfiguration(OMElement configuration);

    /**
     * Practically this is the namespace prefix from the configuration. If we configure
     * a "information-package:process" action, it is nice to actually be able to see
     * this in the log or in exceptions.
     */
    String getAlias();
}

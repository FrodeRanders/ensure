/*
 * Copyright (C) 2014 Frode Randers
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
package eu.ensure.ppe.model;

/**
 * Models what happens to a copy of an object accepted into an aggregation from a
 * storage perspective.
 * <p>
 * Created by Frode Randers at 2013-02-20 11:38
 */
public class Copy {

    private int copyNumber; // not necessarily zero-based!
    private StoragePlugin storagePlugin;
    private ComputePlugin computePlugin = null;

    public Copy(int order) {
        this.copyNumber = order;
    }

    public int getCopyNumber() {
        return copyNumber;
    }

    public StoragePlugin getStoragePlugin() {
        return storagePlugin;
    }

    public void setStoragePlugin(StoragePlugin storagePlugin) {
        this.storagePlugin = storagePlugin;
    }

    public ComputePlugin getComputePlugin() {
        return computePlugin;
    }

    public void setComputePlugin(ComputePlugin computePlugin) {
        this.computePlugin = computePlugin;
    }

    public boolean hasAssociatedComputePlugin() {
        return null != computePlugin;
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\n      [").append("Copy number=\"").append(copyNumber).append("\" ");
        if (1 == copyNumber) {
            buf.append("(master?) ");
        }
        buf.append("]: ");
        buf.append(storagePlugin);
        if (null != computePlugin) {
            buf.append(computePlugin);
        }
        buf.append("\n");
        return buf.toString();
    }
}

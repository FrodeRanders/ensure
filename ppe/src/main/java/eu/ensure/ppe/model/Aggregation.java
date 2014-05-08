/*
 * Copyright (C) 2012-2014 Frode Randers
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

import java.util.*;

/**
 * Models an aggregation and calculations that pertain to the aggregation level.
 * <p>
 * Objects accepted into an aggregation experiences a lifetime consisting of a series of
 * events in a flow. At each event (such as 'ingest') a series of plugins may be applied to
 * the object.
 *
 * Examples of such plugins run at 'ingest' could be various types of transforms,
 * fixity checks of SIPs, checksum generation for new AIPs, and eventually storage.
 *
 * Since storage diversity may apply, some plugins are specific to individual copies of
 * the object. As plugins may be "strongly associated" - in the meaning that picking a
 * specific storage cloud would imply picking a specific compute cloud - we need to
 * treat these combinations individually. An example could be the pairing of
 * Amazon S3 (storage cloud) with Amazon EC2 (compute cloud).
 *
 * To sum it all up; some plugins generally applies to all objects accepted into an aggregation
 * at some event, while other plugins only apply to a specific copy of the object if diversity
 * is used.
 *
 * As a consequence of this, we will treat storage plugins and compute plugins separately.
 * <p>
 * Created by Frode Randers at 2012-09-10 14:56
 */
public class Aggregation {
    private String id;
    private String name = "";
    private int retentionPeriod = 0; // in years
    private boolean needsAnonymization = false;
    private boolean needsEncryption = false;

    private String xml; // The actual XML for the aggregation - read from the GPP
    private Collection<Purpose> primaryPurposes = new LinkedList<Purpose>();
    private Collection<Purpose> secondaryPurposes = new LinkedList<Purpose>();

    private Collection<String> mimeTypes = null;

    /*
     * Events in the lifetime of an object accepted into this aggregation.
     */
    private Map<String, Event> events = new HashMap<String, Event>();

    /*
     * Data related to copies (i.e. storage facilities and the associated compute facilities)
     */
    private Map<Integer, Copy> copies = new HashMap<Integer, Copy>();

    public Aggregation(String id, String name, Collection<String> mimeTypes, String xml) {
        this.id = id;
        this.name = name;
        this.mimeTypes = mimeTypes;
        this.xml = xml;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getRetentionPeriod() {
        return retentionPeriod;
    }

    public boolean getNeedsAnonymization() {
        return needsAnonymization;
    }

    public void setNeedsAnonymization(boolean value) {
        this.needsAnonymization = value;
    }

    public boolean getNeedsEncryption() {
        return needsEncryption;
    }

    public void setNeedsEncryption(boolean value) {
        this.needsEncryption = value;
    }

    public void setRetentionPeriod(int retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    public Collection<Purpose> getPrimaryPurposes() {
        return Collections.unmodifiableCollection(primaryPurposes);
    }

    public Collection<Purpose> getSecondaryPurposes() {
        return Collections.unmodifiableCollection(secondaryPurposes);
    }

    public void addPurpose(Purpose purpose) {
        if (null == purpose)
            return;

        // Verify uniqueness - naïve implementation (but OK as collection is very small!)
        //   PurposeClass: { EVIDENCE, ... }
        //   PurposeType:  { PRIMARY, SECONDARY }

        switch (purpose.getPurposeType()) {
            case PRIMARY:
                for (Purpose p : primaryPurposes) {
                    if (p.getPurposeClass() == purpose.getPurposeClass()
                     && p.getPurposeType() == purpose.getPurposeType())
                        return; // ignore and return
                }
                primaryPurposes.add(purpose);
                break;

            case SECONDARY:
                for (Purpose p : secondaryPurposes) {
                    if (p.getPurposeClass() == purpose.getPurposeClass()
                     && p.getPurposeType() == purpose.getPurposeType())
                        return; // ignore and return
                }
                secondaryPurposes.add(purpose);
                break;
        }
    }

    public Collection<String> getMimeTypes() {
        return mimeTypes;
    }

    public String getXml() {
        return xml;
    }

    public Map<String, Event> getEvents() {
        return events;
    }

    public Map<Integer, Copy> getCopies() {
        return copies;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\n    [").append("Aggregation \"").append(name).append("\" ");

        String separator = "";
        buf.append("primary-purposes={");
        for (Purpose purpose : primaryPurposes) {
            buf.append(separator).append("\"").append(purpose.getPurposeClass().name().toLowerCase()).append("\"");
            separator = ", ";
        }

        separator = "";
        buf.append("} secondary-purposes={");
        for (Purpose purpose : secondaryPurposes) {
            buf.append(separator).append("\"").append(purpose.getPurposeClass().name().toLowerCase()).append("\"");
            separator = ", ";
        }

        separator = "";
        buf.append("} mime-types={");
        for (String mimeType : mimeTypes) {
            buf.append(separator).append("\"").append(mimeType).append("\"");
            separator = ", ";
        }
        buf.append("}]").append(": ");

        for (Copy copy : copies.values()) {
            buf.append(copy);
        }
        for (Event event : events.values()) {
            buf.append(event);
        }
        buf.append("\n");
        return buf.toString();
    }
}

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

import eu.ensure.ppe.EvaluationException;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Models events in the preservation process, such as obsolescence notices or the like.
 * <p>
 * Created by Frode Randers at 2012-09-10 14:56
 */
public class Event {
    private static final Logger log = Logger.getLogger(Event.class);

    public enum EventType {
        UNKNOWN,
        ON_START,
        ON_INGEST,    // OAIS
        ON_ERROR,
        ON_SEARCH,
        ON_ACCESS,    // OAIS
        ON_END,
        ON_TIME,      // OAIS?
        ON_TRANSFORM
    }

    public static final String UNKNOWN = EventType.UNKNOWN.name();
    public static final String ON_START = EventType.ON_START.name();
    public static final String ON_INGEST = EventType.ON_INGEST.name();
    public static final String ON_ERROR = EventType.ON_ERROR.name();
    public static final String ON_SEARCH = EventType.ON_SEARCH.name();
    public static final String ON_ACCESS = EventType.ON_ACCESS.name();
    public static final String ON_END = EventType.ON_END.name();
    public static final String ON_TIME = EventType.ON_TIME.name();
    public static final String ON_TRANSFORM = EventType.ON_TRANSFORM.name();


    private EventType type;

    private List<Plugin> plugins = new Vector<Plugin>();

    public Event(EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    private static Event parseTrigger(String trigger) throws EvaluationException {
        if (ON_START.equalsIgnoreCase(trigger)) {
            return new Event(EventType.ON_START);
        }
        if (ON_INGEST.equalsIgnoreCase(trigger)) {
            return new Event(EventType.ON_INGEST);
        }
        if (ON_ERROR.equalsIgnoreCase(trigger)) {
            return new Event(EventType.ON_ERROR);
        }
        if (ON_SEARCH.equalsIgnoreCase(trigger)) {
            return new Event(EventType.ON_SEARCH);
        }
        if (ON_ACCESS.equalsIgnoreCase(trigger)) {
            return new Event(EventType.ON_ACCESS);
        }
        if (ON_END.equalsIgnoreCase(trigger)) {
            return new Event(EventType.ON_END);
        }
        if (ON_TIME.equalsIgnoreCase(trigger)) {
            return new Event(EventType.ON_TIME);
        }
        if (ON_TRANSFORM.equalsIgnoreCase(trigger)) {
            return new Event(EventType.ON_TRANSFORM);
        }
        log.warn("Unknown event type: " + trigger);
        return new Event(EventType.UNKNOWN);
    }

    public static Event findEvent(Map<String, Event> events, String trigger) throws EvaluationException {
        Event event = events.get(trigger);
        if (null == event) {
            event = parseTrigger(trigger);
            events.put(trigger, event);
        }
        return event;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\n      [").append("Event ").append(this.type.name()).append("]").append(": ");
        for (Plugin plugin : plugins) {
            buf.append(plugin);
        }
        buf.append("\n");
        return buf.toString();
    }
}

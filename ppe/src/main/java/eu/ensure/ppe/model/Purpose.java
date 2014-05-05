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
 * Models a limited set of known purposes.
 * <p/>
 * Created by Frode Randers at 2013-02-19 16:37
 */
public class Purpose {
    public enum PurposeClass {
        EVIDENCE,
        FISCAL_VALUE,
        RESEARCH,
        HISTORY,
        ADMINISTRATIVE,
        INFORMATIONAL,
        DIAGNOSTIC,
        RUNTIME
    }

    public static final String EVIDENCE = PurposeClass.EVIDENCE.name();
    public static final String FISCAL_VALUE = PurposeClass.FISCAL_VALUE.name();
    public static final String RESEARCH = PurposeClass.RESEARCH.name();
    public static final String HISTORY = PurposeClass.HISTORY.name();
    public static final String ADMINISTRATIVE = PurposeClass.ADMINISTRATIVE.name();
    public static final String INFORMATIONAL = PurposeClass.INFORMATIONAL.name();
    public static final String DIAGNOSTIC = PurposeClass.DIAGNOSTIC.name();
    public static final String RUNTIME = PurposeClass.RUNTIME.name();

    public enum PurposeType {
        PRIMARY,
        SECONDARY
    }

    public static final String PRIMARY = PurposeType.PRIMARY.name();
    public static final String SECONDARY = PurposeType.SECONDARY.name();


    private PurposeClass purposeClass = PurposeClass.EVIDENCE;
    private PurposeType purposeType = PurposeType.PRIMARY;

    public Purpose(PurposeClass purposeClass) {
        this.purposeClass = purposeClass;
    }

    public Purpose(PurposeClass purposeClass, PurposeType purposeType) {
        this.purposeClass = purposeClass;
        this.purposeType = purposeType;
    }

    public PurposeClass getPurposeClass() {
        return purposeClass;
    }

    public PurposeType getPurposeType() {
        return purposeType;
    }

    public String toString() {
        return purposeClass.name();
    }
}

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
package  eu.ensure.commons.lang;

/**
 * A dynamic initializer is used to initiate <i>dynamically loaded</i>
 * objects, see {@link  eu.ensure.commons.lang.DynamicLoader}&lt;C&gt;.
 * <p>
 * Since dynamically loaded objects may not have elaborated
 * constructors (they <b>must</b> have a default constructor),
 * we are depending on calling some kind of initializer
 * subsequent to creating the object instance.
 *
 * This class wraps this behaviour when used together with
 * the {@link  eu.ensure.commons.lang.DynamicLoader}&lt;C&gt;.
 * <p>
 * In the general case, use as follows:
 * <pre>
 *   final String arg0 = "holy";
 *   final String arg1 = "smoke";
 *
 *   <b>DynamicInitializer</b>&lt;<i>SomeClass</i>&gt; <b>di</b> = new <b>DynamicInitializer</b>&lt;<i>SomeClass</i>&gt;() {
 *       public void <b>initialize</b>(<i>SomeClass dynamicObject</i>) {
 *           dynamicObject.initialize(arg0, arg1);
 *       }
 *   };
 * </pre>
 * <p>
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public interface DynamicInitializer<C> {
    void initialize(C dynamicObject);
}

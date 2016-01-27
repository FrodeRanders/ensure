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

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Anything that works upon temporary files. 'java.util.concurrent.Callable' defines the call()
 * method, but as it does not take any parameters we need to roll our own.
 *
 * There are different types of file processors though and depending on the third party software
 * we will be using, sometimes it is more convenient to operate on Streams while in other cases
 * Channels make more sense. Thus we will have specific callables that address these cases:
 * See {@link FileProcessorUsingStreamsCallable}, {@link FileProcessorUsingChannelsCallable}
 */
public interface FileProcessorCallable {
}

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
package  eu.ensure.commons.lang;

import org.apache.log4j.Logger;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dynamically compile a Java program
 * <p/>
 * Adapted from example in http://www.java2s.com/Code/Java/JDK-6/CompilingfromMemory.htm
 * <p/>
 * Created by Frode Randers at 2012-07-28 17:24
 */
public class DynamicCompiler {
    private static final Logger log = Logger.getLogger(DynamicCompiler.class);

    final private File workDirectory;

    public DynamicCompiler(File workingDirectory) throws IOException {
        // Create working directory (usual workaround)
        workDirectory = File.createTempFile("compiler-", "-output", workingDirectory);
        workDirectory.delete();
        if (!workDirectory.mkdir()) {
            String info = "Failed to prepare a working directory for the compiler: " + workDirectory.getAbsolutePath();
            log.warn(info);

            throw new IOException(info);
        }
    }

    public File getDirectory() {
        return workDirectory;
    }

    public boolean compile(String name, StringBuilder code, StringBuilder diagnose) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        JavaFileObject file = new JavaSourceFromString(name, code);

        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
        List<String> optionList = new ArrayList<String>();

        // The compiler's classpath includes the runtime's classpath, but
        // extended with the local work directory of the compiler
        String classPath = System.getProperty("java.class.path");
        classPath += (System.getProperty("os.name").toLowerCase().contains("win")) ? ";" : ":"; // This is silly!
        classPath += workDirectory.getAbsolutePath();

        optionList.addAll(
                Arrays.asList(
                        "-classpath", classPath,       // Classpath also contains workDirectory
                        "-d", workDirectory.getPath(), // Compiler output goes here
                        "-s", workDirectory.getPath(), // Compiler output goes here
                        "-Xlint"
                        // "-verbose"
                )
        );

        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, optionList, null, compilationUnits);

        boolean success = task.call();
        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
            diagnose.append(diagnostic.getKind()).append(": ")
                    .append(diagnostic.getCode()).append(" ")
                    //.append(diagnostic.getPosition()).append(" ")
                    //.append(diagnostic.getStartPosition()).append(" ")
                    //.append(diagnostic.getEndPosition()).append(" ")
                    .append(diagnostic.getSource()).append(" ")
                    .append(diagnostic.getMessage(null));

        }
        return success;
    }

    private class JavaSourceFromString extends SimpleJavaFileObject {
        final StringBuilder code;

        public JavaSourceFromString(String name, StringBuilder code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}


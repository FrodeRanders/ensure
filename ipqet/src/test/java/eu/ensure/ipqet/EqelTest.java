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
package eu.ensure.ipqet;

import eu.ensure.commons.io.Closer;
import eu.ensure.commons.io.FileIO;
import eu.ensure.commons.lang.DynamicCompiler;
import eu.ensure.commons.lang.DynamicInvoker;
import eu.ensure.commons.lang.LoggingUtils;
import eu.ensure.commons.lang.Stacktrace;
import eu.ensure.ipqet.eqel.EqelLoader;
import eu.ensure.ipqet.eqel.model.DomainSpecification;
import eu.ensure.ipqet.eqel.model.ValidationSpecification;
import junit.framework.TestCase;

import org.antlr.stringtemplate.*;

import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.*;
import java.util.*;

/**
 */
public class EqelTest extends TestCase {
    private static final Logger log = LoggingUtils.setupLoggingFor(EqelTest.class, "log-configuration.xml");

    @Test
    public void testEqelParser()
    {
		InputStream is = null;
		try {
			is = getClass()./* getClassLoader(). */ getResourceAsStream("simple.eqel");

            final Map<String, DomainSpecification> domainSpecifications = new HashMap<String, DomainSpecification>();
            final Map<String, ValidationSpecification> validationSpecifications = new HashMap<String, ValidationSpecification>();

            EqelLoader loader = new EqelLoader(domainSpecifications, validationSpecifications, this.getClass());
            loader.load(is);
        }
        catch (Throwable t) {
			fail("Failed to parse EQEL: " + t.getMessage());
		}
        finally {
            Closer.close(is);
        }
    }


    @Test
    public void testEqelCodegen()
    {
        System.out.println("\nTesting simple code generation:");

        // Simple template test
        String decl =
            "int[] values = { $values; separator=\", \"$ };";
        StringTemplate st = new StringTemplate(decl);
        st.setAttribute("values", "42");
        st.setAttribute("values", "43");
        st.setAttribute("values", "44");
        System.out.println(st);

        System.out.println("\nTesting code generation:");

        // A more elaborate string template test
		InputStream is = null;
		try {
            // is = Thread.currentThread().getContextClassLoader().getResourceAsStream("test.st");
			is = getClass().getResourceAsStream("test.st");
            Reader reader = new InputStreamReader(is);

            StringBuilder src = new StringBuilder();

            // Preamble
            StringTemplateGroup group =  new StringTemplateGroup(reader, DefaultTemplateLexer.class);
            StringTemplate preamble = group.getInstanceOf("preamble");
            src.append(preamble.toString());

            // Some code from a template
            String packageName = "test";
            String className = "Test";

            StringTemplate block = group.getInstanceOf("block");
            block.setAttribute("package", packageName);
            block.setAttribute("name", className);

            block.setAttribute("strings", "Alpha");
            block.setAttribute("strings", "Beta");
            block.setAttribute("strings", "Gamma");
            block.setAttribute("strings", "Delta");
            block.setAttribute("doViewCompileTimeData", true);

            System.out.println(block.toString());

            System.out.println("\nTesting generated code execution:");
            src.append(block.toString());

            File cwd = new File(System.getProperty("user.dir"));
            DynamicCompiler compiler = new DynamicCompiler(cwd);

            StringBuilder diagnostics = new StringBuilder();
            if (compiler.compile(className, src, diagnostics)) {
                System.out.println(diagnostics.toString());

                // Parameter types and values
                Vector<String> _parameters = new Vector<String>();
                _parameters.add("param1");
                _parameters.add("param2");
                _parameters.add("param3");

                Object[] parameters = { _parameters };
                Class[] types = { List.class };

                // Invoke method
                DynamicInvoker invoker = new DynamicInvoker(compiler.getDirectory(), "testprogram");
                invoker.invoke(packageName + "." + className, "test", parameters, types);

                // Remove compiler working directory
                FileIO.delete(compiler.getDirectory());
            } else {
                System.out.println(diagnostics.toString());
            }
        }
        catch (Throwable t) {
            System.out.println(Stacktrace.asString(t));
            Throwable baseCause = Stacktrace.getBaseCause(t);
			fail("Failed to run string template test: " + baseCause.getMessage());
		}
        finally {
            try {
                if (null != is) is.close();
            }
            catch (IOException ignore) {
            }
        }
    }
}

/*
import java.io.IOException;
import java.util.Arrays;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class DiagnosticCollectorCompile {

  public static void main(String args[]) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
    Iterable<? extends JavaFileObject> compilationUnits = fileManager
        .getJavaFileObjectsFromStrings(Arrays.asList("Foo.java"));
    JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null,
        null, compilationUnits);
    boolean success = task.call();
    fileManager.close();
    System.out.println("Success: " + success);
  }
}

// File: MyClass.java
class MyClass {
  public static void main(String args[]) {
    System.out.println("Hello, World");
  }
}
*/
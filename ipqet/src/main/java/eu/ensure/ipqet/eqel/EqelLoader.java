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
package eu.ensure.ipqet.eqel;

import eu.ensure.vopn.io.Closer;
import eu.ensure.vopn.io.FileIO;
import eu.ensure.ipqet.eqel.model.DomainSpecification;
import eu.ensure.ipqet.eqel.model.ValidationSpecification;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

/**
 * Description of LexerRule:
 * <p>
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class EqelLoader {
    private final Map<String, DomainSpecification> domainSpecifications;
    private final Map<String, ValidationSpecification> validationSpecifications;
    private final Class referenceClass;

    public EqelLoader(
            final Map<String, DomainSpecification> domainSpecifications,
            final Map<String, ValidationSpecification> validationSpecifications,
            final Class referenceClass
    ) {
        this.domainSpecifications = domainSpecifications;
        this.validationSpecifications = validationSpecifications;
        if (null != referenceClass) {
            this.referenceClass = referenceClass;
        } else {
            this.referenceClass = this.getClass();
        }
    }

    public /* aggregation tree */ void load(String reference) throws IOException, RecognitionException {

        if (reference.startsWith("file:")) {
            File file = new File(reference.substring(/* lengthOf(file:) */ 5));
            load(file);
        }
        else if (reference.startsWith("http:")) {
            URL url = new URL(reference);
            File file = FileIO.getRemoteFile(url, /* keepAlive? */ false);
            load(file);
        }
        else if (reference.startsWith("classpath:")) {
            InputStream is = null;
            try {
                is = referenceClass.getResourceAsStream(reference.substring(/* lengthOf("classpath:") */ 10));
                if (null == is) {
                    String info = "Could not locate EQEL resource \"" + reference + "\" relative to ";
                    info += referenceClass.getCanonicalName();
                    throw new EqelException(info);
                }
                load(is);
            }
            finally {
                Closer.close(is);
            }
        }
        else {
            File file = new File(reference);
            load(file);
        }
    }

    public /* aggregation tree */ void load(File eqelFile) throws IOException, RecognitionException {

       	if (null == eqelFile || !eqelFile.exists()) {
            String info = "Could not operate on non-existing file";
            if (null != eqelFile) {
                info += ": " + eqelFile.getAbsolutePath();
            }
            throw new IOException(info);
       	}

        InputStream is = null;
        try {
            is = new FileInputStream(eqelFile);
            load(is);

        } finally {
            Closer.close(is);
        }
    }

    public /* aggregation tree */ void load(InputStream is) throws IOException, RecognitionException {
        ANTLRInputStream input = new ANTLRInputStream(is);

        /*
         * NOTE! EqelLexer and EqelParser are auto-generated. You have to build the project once
         *       if the syntax highlighter is indicating that these classes don't exist.
         */
        EqelLexer lexer = new EqelLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        EqelParser parser = new EqelParser(tokens);
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        ParserRuleContext tree = parser.statements(); // parse

        ParseTreeWalker walker = new ParseTreeWalker(); // a standard walker
        Listener listener = new Listener(parser, domainSpecifications, validationSpecifications, this);
        walker.walk(listener, tree); // initiate walk to tree with listener
    }
}
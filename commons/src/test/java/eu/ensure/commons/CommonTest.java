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
package  eu.ensure.commons;

import eu.ensure.commons.statistics.MovingAverage;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import java.math.BigInteger;
import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;

import eu.ensure.commons.lang.TimeDelta;

/**
 * <p/>
 * Created by Frode Randers at 2012-10-23 11:00
 */
public class CommonTest extends TestCase {
    private static Logger log = null;

    static {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            /*
            docBuilderFactory.setValidating(false);
            docBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
            docBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/validation/schema", false);
            */
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            docBuilder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId)
                        throws SAXException, IOException {
                    if (systemId.contains("log4j.dtd")) {
                        return new InputSource(new StringReader(""));
                    } else {
                        return null;
                    }
                }
            });
            InputStream config = null;
            try {
                config = CommonTest.class.getResourceAsStream("log-configuration.xml");
                Document doc = docBuilder.parse(config);
                DOMConfigurator.configure(doc.getDocumentElement());
                log = Logger.getLogger(CommonTest.class);
                log.info("Logging commences...");

            } finally {
                if (null != config) config.close();
            }
        } catch (Exception e) {
        }
    }

    @Test
    public void testProviders() {
        try {
          Provider p[] = Security.getProviders();
          for (int i = 0; i < p.length; i++) {
              System.out.println("Provider: " + p[i]);
              for (Enumeration e = p[i].keys(); e.hasMoreElements();) {
                  Object key = e.nextElement();
                  if (key.toString().startsWith("MessageDigest")) {
                      System.out.println("  * " + key);
                  }
                  else {
                      System.out.println("    " + key);
                  }
              }
          }
        } catch (Exception e) {
          System.out.println(e);
        }
    }

    @Test
    public void testTimeDelta() {
        System.out.println("\nTesting human readable time periods (millisecs to text):");

        long halfASecond = 500; 
        System.out.println("500 ms -> " + TimeDelta.asHumanApproximate(BigInteger.valueOf(halfASecond)));

        long oneAndAHalfSecond = 1500;
        System.out.println("1500 ms -> " + TimeDelta.asHumanApproximate(BigInteger.valueOf(oneAndAHalfSecond)));

        long sixtyFiveSeconds = 65 * 1000;
        System.out.println("65 s -> " + TimeDelta.asHumanApproximate(BigInteger.valueOf(sixtyFiveSeconds)));

        long sixtyFiveMinutes = 65 * 60 * 1000;
        System.out.println("65 min -> " + TimeDelta.asHumanApproximate(BigInteger.valueOf(sixtyFiveMinutes)));

        long twentyFiveHours = 25 * 60 * 60 * 1000;
        System.out.println("25 hours -> " + TimeDelta.asHumanApproximate(BigInteger.valueOf(twentyFiveHours)));

        BigInteger thirtyFiveDays = BigInteger.valueOf(35).multiply(BigInteger.valueOf(24 * 60 * 60 * 1000));
        System.out.println("35 days -> " + TimeDelta.asHumanApproximate(thirtyFiveDays));

        BigInteger thirteenMonths = BigInteger.valueOf(13 * 30).multiply(BigInteger.valueOf(24 * 60 * 60 * 1000));
        System.out.println("13 months -> " + TimeDelta.asHumanApproximate(thirteenMonths));

        BigInteger tenYears = BigInteger.valueOf(10 * 12 * 30).multiply(BigInteger.valueOf(24 * 60 * 60 * 1000));
        System.out.println("10 years -> " + TimeDelta.asHumanApproximate(tenYears));
    }

    @Test
    public void testStatistics() {
        {
            MovingAverage ma1 = new MovingAverage();

            int sum = 0;
            int i;

            for (i = 0; i < 100; i++) {
                sum += i;
                ma1.update(i); // cast
            }

            assertEquals(((double) sum)/i, ma1.getAverage());
            assertEquals(i, ma1.getCount());
        }
        {
            MovingAverage ma2 = new MovingAverage();
            int[] samples = {3, 7, 5, 13, 20, 23, 39, 23, 40, 23, 14, 12, 56, 23, 29};
            for (int sample : samples) {
                ma2.update(sample); // cast
            }

            double sum = 0;
            double average = ma2.getAverage();
            for (int sample : samples) {
                sum += Math.pow(sample - average, 2);
            }
            double stdDev = Math.sqrt(sum / (samples.length - 1));

            assertEquals(22.0, ma2.getAverage());
            assertEquals(samples.length, ma2.getCount());
            assertEquals(stdDev, ma2.getStdDev(), /* acceptable delta */ 1E-13);
        }
        {
            double sum = 0.0;
            int i;

            MovingAverage ma3 = new MovingAverage();
            for (i = 0; i < 100; i++) {
                double sample = Math.random();
                ma3.update(sample);
                sum += sample;
            }

            assertEquals(sum/i, ma3.getAverage(), /* acceptable delta */ 1E-15);
            assertEquals(i, ma3.getCount());
        }
    }
}

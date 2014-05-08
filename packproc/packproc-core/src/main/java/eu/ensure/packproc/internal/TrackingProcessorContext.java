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
package eu.ensure.packproc.internal;

import eu.ensure.packproc.model.AssociatedInformation;
import eu.ensure.packproc.model.EvaluationStatement;
import eu.ensure.packproc.model.ProcessorContext;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Description of TrackingProcessorContext:
 * <p>*
 * Created by Frode Randers at 2011-12-14 00:34
 */
public class TrackingProcessorContext implements ProcessorContext {
    private static final Logger log = Logger.getLogger(TrackingProcessorContext.class);

    // This is a collection of statements regarding the state of the information package
    private static Collection<EvaluationStatement> evaluationStatements =
            new LinkedList<EvaluationStatement>();

    // This is a collection of file information
    private static Map<String, TrackedAssociatedInformation> associatedInfo =
            new HashMap<String, TrackedAssociatedInformation>();

    // This is the stack capturing the 'current context'. Contexts are push:ed
    // and pop:ed on this stack and the topmost is always current.
    private static final Stack<ProcessorContext> stack = new Stack<ProcessorContext>();

    // This is a collection of all 'prior sub contexts' of a context.
    // It will be possible to traverse the information hierarchy of a structure
    // _after_ it was fully processed - not necessarily during processing though.
    private final Collection<ProcessorContext> children = new LinkedList<ProcessorContext>();

    // Name of entity that context clings to
    private String name;

    public TrackingProcessorContext(String name) {
        this.name = name;
    }

    private TrackingProcessorContext(ProcessorContext previous) {
        stack.push(previous);
    }

    public String getContextName() {
        return name;
    }

    // Methods that handle the 'current context' stack
    public <E extends ProcessorContext> E push(E previous) {
        stack.push(previous);
        return previous;
    }

    public ProcessorContext pop() {
        if (stack.empty()) {
            String info = "[synthetic exception] Mismatched push/pop on context stack: ";
            info += "Current context belongs to " + getContextName();
            Exception about = new Exception(info);
            log.warn(about);

            // Now, let it crash!
        }
        ProcessorContext childContext = stack.pop();
        addChild(childContext);
        return childContext;
    }

    public Collection<ProcessorContext> getContextStack() {
        return Collections.unmodifiableCollection(stack);
    }

    public int getDepth() {
        return stack.size();
    }

    public int getDepth(String className) {
        int depth = 0;

        Collection<ProcessorContext> var = getContextStack();
        ProcessorContext[] ary = var.toArray(new ProcessorContext[var.size()]);
        for (int i=ary.length-1; i >= 0; i--) {
            if (ary[i].getClass().getCanonicalName().equals(className)) {
                ++depth;
            }
        }
        return depth;
    }

    // Methods that handle the information hierarchy
    public void addChild(ProcessorContext child) {
        children.add(child);
    }

    public Collection<ProcessorContext> getChildren() {
        return Collections.unmodifiableCollection(children);
    }


    // Methods that handle collected information in a context
    public Map<String, String> getCollectedValues() {
        return new HashMap<String, String>(); // empty map
    }

    // Methods that handle information on a per-file basis
    public void associate(final String claimant, final String path, final String providedPath, final Map<String, String> providedValues) {

        if (!path.equals(providedPath)) {
            String statement = claimant + " does not correctly refer to file within the information package. ";
            statement += "The provided path was \"" + providedPath + "\"";
            evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.NEGATIVE, statement));
        }

        if (! associatedInfo.containsKey(path)) {
            // New information for this path
            TrackedAssociatedInformation assocInfo = new TrackedAssociatedInformation(claimant, path, providedValues);
            associatedInfo.put(path, assocInfo);

            //
            for (String key : providedValues.keySet()) {
                String statement = claimant + " states that ";
                statement += key + "=\"" + providedValues.get(key) + "\"";
                evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.NEUTRAL, statement));
            }
        } else {
            // Additional information for this path
            TrackedAssociatedInformation assocInfo = associatedInfo.get(path);
            Map</* key */ String, Map</* value */ String, /* claimants */ Set<String>>> existingValues = assocInfo.getValues();

            boolean isAffirmative = true;
            for (String newKey : providedValues.keySet()) {

                // Ignore null-values
                String newValue = providedValues.get(newKey);
                if (null == newValue) {
                    continue; // and ignore
                }

                if (!existingValues.containsKey(newKey)) {
                    // Is it only the case that differs?
                    boolean differsInCaseOnly = false;
                    for (String key : existingValues.keySet()) {
                        if (key.equalsIgnoreCase(newKey)) {
                            // Ouch!
                            String info = "A key already exists that seems to be spelled slightly different ";
                            info += "- adjusted to match; ";
                            info += " Key changed from \"" + newKey + "\" to \"" + key + "\"";
                            log.warn(info);

                            //
                            String statement = claimant + " states that the key \"";
                            statement += newKey + "\" is similar, but not equal, to previously used key \"" + key + "\"";
                            evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.NEGATIVE, statement));

                            newKey = key; // Adjust key so that we don't miss this!

                            differsInCaseOnly = true;
                            break;
                        }
                    }
                    if (!differsInCaseOnly) {
                        // We have no existing values for this key yet
                        TrackedAssociatedInformation.addValueTo(existingValues, newKey, newValue, claimant);

                        //
                        String statement = claimant + " states that ";
                        statement += newKey + "=\"" + newValue + "\"  ";
                        evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.NEUTRAL, statement));

                        continue; // with next key/value
                    }
                }

                switch (TrackedAssociatedInformation.addValueTo(existingValues, newKey, newValue, claimant)) {
                    case COINCIDING_VALUE:
                        {
                            if (log.isDebugEnabled()) {
                                String info = "@ Affirmation from " + claimant + ": ";
                                info += path;
                                info += " has " + newKey + "=" + newValue;
                                log.debug(info);
                            }

                            String statement = claimant + " confirms that ";
                            statement += newKey + "=\"" + newValue + "\"  ";
                            evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.POSITIVE, statement));
                        }
                        break;

                    case NEW_VALUE:
                        {
                            String statement = claimant + " states that ";
                            statement += newKey + "=\"" + newValue + "\"  ";
                            evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.NEUTRAL, statement));
                        }
                        break;

                    case CONFLICTING_VALUE:
                        {
                            isAffirmative = false;

                            String statement = claimant + " contests prior claims regarding ";
                            statement += path;
                            statement += ". We now have these conflicting values for key \"" + newKey + "\": ";

                            Map</* value */ String, /* claimants */ Set<String>> values = existingValues.get(newKey);
                            Iterator<String> vit = values.keySet().iterator();
                            while (vit.hasNext()) {
                                String value = vit.next();
                                statement += "\"" + value + "\" ";
                                {
                                    statement += "[";
                                    Iterator</* claimant */ String> cit = values.get(value).iterator();
                                    while (cit.hasNext()) {
                                        statement += /* claimant */ cit.next();
                                        if (cit.hasNext()) {
                                            statement += ", ";
                                        }
                                    }
                                    statement += "]";
                                }
                                if (vit.hasNext()) {
                                    statement += ", ";
                                }
                            }

                            log.warn(statement);
                            evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.NEGATIVE, statement));
                        }
                        break;
                }
            }
        }
    }

    public Collection<? extends AssociatedInformation> extractAssociatedInformation() {
        Collection<? extends AssociatedInformation> assocInfo = associatedInfo.values();

        // Prepare for next
        associatedInfo = new HashMap<String, TrackedAssociatedInformation>();
        return assocInfo;
    }

    public static Collection<EvaluationStatement> getEvaluationStatements() {
        return evaluationStatements;
    }

    public static Collection<EvaluationStatement> extractEvaluationStatements() {
        Collection<EvaluationStatement> statements = evaluationStatements;

        // Prepare for next
        evaluationStatements = new LinkedList<EvaluationStatement>();
        return statements;
    }

    public static void debugEvaluationStatements(Collection<EvaluationStatement> statements) {
        log.info("");
        log.info("---------------------------------------------------------------------------");
        log.info(" Evaluation statements");
        log.info("---------------------------------------------------------------------------");
        for (EvaluationStatement statement : statements) {
            String path = statement.getPath();
            String factor = "" + statement.getFactor();
            String statemt = statement.getStatement();

            log.info("     Path: " + path);
            log.info("   Factor: " + factor);
            log.info("Statement: " + statemt);
            log.info("");
        }
        log.info("---------------------------------------------------------------------------");
        log.info("");
    }

    public static void debugAssociatedInformation(Collection<AssociatedInformation> assocInfos) {
        log.info("");
        log.info("---------------------------------------------------------------------------");
        log.info(" Information gathered from information package");
        log.info("---------------------------------------------------------------------------");
        for (AssociatedInformation assocInfo : assocInfos) {
            String path = assocInfo.getPath();
            Map</* key */ String, Map</* value */ String, /* claimants */ Set<String>>> values = assocInfo.getValues();

            log.info("  Path: " + path);
            for (String key : values.keySet()) {
                Map</* value */ String, /* claimants */ Set<String>> assocValue = values.get(key);

                // Iterate over all (possible) values
                for (String value : assocValue.keySet()) {
                    String info = "  * " + key + " = ";
                    info += value;

                    // Annotating value with claimants
                    info += " (";
                    Iterator<String> claimants = assocValue.get(value).iterator();
                    while (claimants.hasNext()) {
                        String claimant = claimants.next();
                        info += claimant;
                        if (claimants.hasNext()) {
                            info += ", ";
                        }
                    }
                    info += ")";
                    log.info(info);
                }
            }
            log.info("");
        }
        log.info("---------------------------------------------------------------------------");
        log.info("");
    }

    public static void debugContextHierarchy(ProcessorContext ctx) {
        String name = ctx.getContextName(); // may be null!
        if (null == name || name.length() == 0) {
            // We have entered an uninteresting sub-context - ignore
            return;
        }

        log.info("--------------------------------------------------------------------------------");
        log.info("-- " + ctx.getClass().getName());

        log.info("-- " + name);

        Map<String, String> collectedValues = ctx.getCollectedValues();
        for (String key : collectedValues.keySet()) {
            String value = collectedValues.get(key);
            // TODO Check this
            /*
            if (ctx instanceof DicomProcessorContext) {

                log.info("-> " + ValueHandler.getTagInfo(key) + (null != value ? value : ""));
            } else {
                log.info("-> " + key + ": " + value);
            }
            */
        }

        Collection<ProcessorContext> children = ctx.getChildren();
        for (ProcessorContext child : children) {
            debugContextHierarchy(child);
        }
    }
}

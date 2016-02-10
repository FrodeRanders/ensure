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
package eu.ensure.ipqet.eqel;

import eu.ensure.ipqet.eqel.model.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description of Listener.
 * <p>
 * Created by Frode Randers at 2014-01-22 21:38
 */
public class Listener extends EqelBaseListener {
    private static final String UNWRAP_RE = "\"((?:.*?))\"";
    private static final Pattern unwrapPattern = Pattern.compile(UNWRAP_RE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final EqelParser parser;

    private final Map<String, DomainSpecification> domainSpecifications;
    private final Map<String, ValidationSpecification> validationSpecifications;
    private final EqelLoader loader;

    public Listener(EqelParser parser,
                    final Map<String, DomainSpecification> domainSpecifications,
                    final Map<String, ValidationSpecification> validationSpecifications,
                    final EqelLoader loader) {

        this.parser = parser;
        this.domainSpecifications = domainSpecifications;
        this.validationSpecifications = validationSpecifications;
        this.loader = loader;
    }

    private String unwrap(String s) {
        Matcher m = unwrapPattern.matcher(s);
        if (m.find()) {
            return m.group(1);
        }
        return s;
    }

    /**
     *
     */
   @Override
   public void enterIMPORT(EqelParser.IMPORTContext ctx) {
        EqelParser.ImportStmtContext stmt = ctx.importStmt();

        // importStmt : 'import' STRING
        String eqelFile = stmt.STRING().getText();

        try {
            loader.load(unwrap(eqelFile));
        }
        catch (IOException ioe) {
            String info = "Failed to load EQEL file \"" + eqelFile + "\": ";
            info += ioe.getMessage();
            throw new EqelException(info, ioe);
        }
    }

    /**
     */
   @Override
   public void enterVALIDATION(EqelParser.VALIDATIONContext ctx) {
        EqelParser.ValidationSpecContext validationSpec = ctx.validationSpec();

        // validationSpec : 'validate' name=IDENT 'using' CLASSNAME '{' validationVerb* '}'
        final String validationIdent = validationSpec.IDENT().getText();
        final String className = validationSpec.CLASSNAME().getText();

        //
        ValidationSpecification _validationSpecification = new ValidationSpecification(validationIdent, className);

        List<EqelParser.ValidationVerbContext> validationVerbs = validationSpec.validationVerb();
        for (EqelParser.ValidationVerbContext validationVerb : validationVerbs) {
            // validationVerb  : 'verify' verb '{' verificationStmt* '}'
            final String verb = validationVerb.verb().getText();
            ValidationVerb _validationVerb = _validationSpecification.getVerb(verb);
            Collection<VerificationSpecification> _verificationSpecifications = _validationVerb.getVerificationSpecifications();

            List<EqelParser.VerificationStmtContext> verificationStmts = validationVerb.verificationStmt();
            for (EqelParser.VerificationStmtContext verificationStmt : verificationStmts) {
                // verificationStmt  : name=IDENT '{' verificationAction* '}'
                final String verificationIdent = verificationStmt.name.getText();

                //
                VerificationSpecification _verificationSpecification = new VerificationSpecification(verificationIdent);
                _verificationSpecifications.add(_verificationSpecification);

                List<EqelParser.VerificationActionContext> verificationActions = verificationStmt.verificationAction();
                for (EqelParser.VerificationActionContext verificationAction : verificationActions) {
                    // verificationAction : 'file' pattern=STRING '{' action=STRING* '}'
                    final String pattern = verificationAction.pattern.getText();

                    //
                    ActionSpecification _actionSpecification = new ActionSpecification(pattern);
                    _verificationSpecification.addActionSpecification(_actionSpecification);

                    // NOTE: Since both pattern and actions are STRING tokens, this list will
                    //       also contain the pattern. Step past it when iterating over actions.
                    List<TerminalNode> actions = verificationAction.STRING();
                    for (TerminalNode action : actions.subList(1, actions.size())) {
                        final String text = action.getText();

                        //
                        Action _action = new Action(text);
                        _actionSpecification.addAction(_action);
                    }
                }
            }
        }
        //
        validationSpecifications.put(validationIdent, _validationSpecification);
        System.out.println(_validationSpecification);
    }

    /**
     */
   @Override
   public void enterDOMAIN(EqelParser.DOMAINContext ctx) {
        EqelParser.DomainSpecContext domainSpec = ctx.domainSpec();

        // domainSpec : 'domain' domain=IDENT ('extends' baseDomain=IDENT)? '{' purposeSpec* '}'
        List<TerminalNode> optionalBaseDomain = domainSpec.IDENT();
        final String domain = optionalBaseDomain.get(0).getText();

        boolean inherits = optionalBaseDomain.size() > 1;
        String baseDomain = null;
        if (inherits) {
            baseDomain = optionalBaseDomain.get(1).getText();
        }

        //
        DomainSpecification _domainSpecification = new DomainSpecification(domain);
        if (inherits) {
            DomainSpecification _parentSpecification = domainSpecifications.get(baseDomain);
            if (null == _parentSpecification) {
                String info = "Domain \"" + domain + "\" inherits from domain \"" + baseDomain;
                info += "\" but the base domain is unknown at this time";
                throw new EqelException(info);
            }
            _domainSpecification.setParent(_parentSpecification);
        }

        List<EqelParser.PurposeSpecContext> purposeSpecs = domainSpec.purposeSpec();
        for (EqelParser.PurposeSpecContext purposeSpec : purposeSpecs) {
            // purposeSpec : 'purpose' purpose=IDENT '{' requirementSpec* '}'
            final String purpose = purposeSpec.purpose.getText();

            Purpose _purpose = new Purpose(purpose);
            _domainSpecification.addPurpose(_purpose);

            List<EqelParser.RequirementSpecContext> requirementSpecs = purposeSpec.requirementSpec();
            for (EqelParser.RequirementSpecContext requirementSpec : requirementSpecs) {
                // requirementSpec : requirementType? reference=COMPOUND_IDENT requirement=IDENT

                EqelParser.RequirementTypeContext requirementType = requirementSpec.requirementType();
                String type = null;
                if (null != requirementType) {
                    type = requirementType.getText(); // mandatory or optional?
                }
                final String reference = requirementSpec.reference.getText(); // IP:contains
                final String requirement = requirementSpec.IDENT().getText(); // fixity-information

                Requirement _requirement = new Requirement(reference, requirement, type);
                _purpose.addRequirement(_requirement);

            }
        }
        //
        domainSpecifications.put(domain, _domainSpecification);
        System.out.println(_domainSpecification);
    }
}

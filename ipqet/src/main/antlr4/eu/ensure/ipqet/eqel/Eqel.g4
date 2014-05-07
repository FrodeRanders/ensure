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
grammar Eqel;

statements 
  : statement+ EOF 
  ;

statement 
  : importStmt      # IMPORT
  | domainSpec      # DOMAIN
  | validationSpec  # VALIDATION
  ;

importStmt 
  : 'import' STRING
  ;

domainSpec 
  : 'domain' domain=IDENT ('extends' baseDomain=IDENT)? '{' purposeSpec* '}' 
  ;

purposeSpec 
  : 'purpose' purpose=IDENT '{' requirementSpec* '}' 
  ;

requirementSpec 
  : requirementType? reference=COMPOUND_IDENT requirement=IDENT 
  ;

requirementType 
  : '[mandatory]' | '[optional]' 
  ;

validationSpec 
  : 'validate' name=IDENT 'using' CLASSNAME '{' validationVerb* '}' 
  ;

validationVerb 
  : 'verify' verb '{' verificationStmt* '}' 
  ;

verb 
  : 'contains' 
  | 'has' 
  ;

verificationStmt 
  : name=IDENT '{' verificationAction* '}' 
  ;

verificationAction 
  : 'file' pattern=STRING '{' action=STRING* '}' 
  ;

IDENT 
  :   ( '_' | 'a'..'z' | 'A'..'Z' ) ( '_' | '-' | 'a'..'z' | 'A'..'Z' | '0'..'9' )* 
  ;

COMPOUND_IDENT 
  : IDENT ':' IDENT 
  ;

CLASSNAME 
  : IDENT ( '.' IDENT )* 
  ;

STRING 
  : '"' .*? '"' 
  | '\'' .*? '\'' 
  ;

SL_COMMENT 
  : '//' .*? '\r'? '\n' -> skip 
  ;

COMMENT 
  : '/*' .*? '*/' -> skip 
  ;

WS 
  : ( ' ' | '\t' | '\r' | '\n' | '\f' )+ -> skip 
  ;



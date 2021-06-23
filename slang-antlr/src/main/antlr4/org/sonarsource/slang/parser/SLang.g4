grammar SLang;

slangFile
  :  packageDeclaration? importDeclaration* typeDeclaration* EOF
  ;

packageDeclaration
  : PACKAGE identifier SEMICOLON
  ;

importDeclaration
  : IMPORT identifier SEMICOLON
  ;

typeDeclaration
  :  classDeclaration
  |  methodDeclaration
  |  controlBlock SEMICOLON
  ;

classDeclaration
  :  annotation* CLASS identifier? LCURLY typeDeclaration* RCURLY
  ;

methodDeclaration
  :  annotation* methodModifier* methodHeader methodBody
  ;

annotation
  : AT identifier (LPAREN annotationParameters RPAREN)?
  ;

annotationParameters
  : annotationParameter (COMMA annotationParameter)*
  ;

annotationParameter
  : literal
  | identifier assignmentOperator annotationParameterList
  ;

annotationParameterList
  : literal
  | LCURLY literal (COMMA literal)* RCURLY
  ;

methodModifier
  : PUBLIC  
  | PRIVATE
  | OVERRIDE
  | nativeExpression
  ;

methodHeader
  :  simpleType? FUN methodDeclarator
  ;

methodDeclarator
  :  identifier? LPAREN formalParameterList? RPAREN
  ;

formalParameterList
  :  formalParameters COMMA lastFormalParameter
  |  lastFormalParameter
  |  receiverParameter
  ;

formalParameters
  :  formalParameter (COMMA formalParameter)*
  ;

formalParameter
  :  annotation* parameterModifier* simpleType? variableDeclaratorId ('=' expression)?
  ;

parameterModifier
  : nativeExpression
  ;

lastFormalParameter
  :  simpleType? ELLIPSIS variableDeclaratorId
  |  formalParameter
  ; 

receiverParameter
  :  simpleType? (identifier DOT)? THIS
  ;

variableDeclaratorId
  :  identifier 
  ;

methodBody
  : block 
  | SEMICOLON
  ;

block
  :  LCURLY (statement semi)* (statement semi?)? RCURLY
  ;

statement
  :  declaration
  |  assignment
  |  expression
  ;

declaration
  :  annotation* simpleType? declarationModifier identifier ('=' expression)?
  ;

declarationModifier
  : VAR
  | VAL
  ;

assignment
  :  expression (assignmentOperator statement)+
  ;

expression
  :  disjunction
  ;

disjunction
  :  conjunction (disjunctionOperator conjunction)*
  ;

conjunction
  :  equalityComparison (conjunctionOperator equalityComparison)*
  ;

equalityComparison
  :  comparison (equalityOperator comparison)*
  ;

comparison
  :  additiveExpression (comparisonOperator additiveExpression)*
  ;

additiveExpression
  :  multiplicativeExpression (additiveOperator multiplicativeExpression)*
  ;

multiplicativeExpression
  :  unaryExpression (multiplicativeOperator unaryExpression)*
  ;

unaryExpression
  :  unaryOperator unaryExpression
  |  atomicExpression
  ;

atomicExpression
  :  parenthesizedExpression 
  |  nativeExpression
  |  methodDeclaration
  |  classDeclaration
  |  literal
  |  conditional
  |  loopExpression
  |  methodInvocation
  |  returnExpression
  |  expressionName
  |  tryExpression
  |  jumpExpression
  |  throwExpression
  ;

parenthesizedExpression
  :  LPAREN statement RPAREN
  ;

methodInvocation
  :  memberSelect LPAREN argumentList? RPAREN
  ;

memberSelect
  :  memberSelect DOT identifier
  |  identifier
  ;

argumentList
  :  statement (COMMA statement)*
  ; 

expressionName
  :  memberSelect
  ;

conditional
  :  ifExpression
  |  matchExpression
  ;

ifExpression
  : IF LPAREN statement RPAREN controlBlock (ELSE controlBlock)?
  ;

matchExpression
  : MATCH LPAREN statement? RPAREN LCURLY matchCase* RCURLY
  ;

matchCase
  :  statement ARROW controlBlock? semi
  |  ELSE ARROW controlBlock? semi
  ;

loopExpression
  :  forLoop
  |  whileLoop
  |  doWhileLoop
  ;

forLoop
  :  FOR (LPAREN declaration RPAREN)? controlBlock
  ;

whileLoop
  :  WHILE LPAREN statement RPAREN controlBlock
  ;

doWhileLoop
  :  DO controlBlock WHILE LPAREN statement RPAREN
  ;

controlBlock
  :  block
  |  statement
  ;

tryExpression
  : TRY block catchBlock* finallyBlock?
  ;

catchBlock
  : CATCH LPAREN formalParameter? RPAREN block
  ;

finallyBlock
  : FINALLY block
  ;

nativeExpression
  :  NATIVE LBRACK argumentList? RBRACK LCURLY nativeBlock* RCURLY 
  ; 

nativeBlock
  :  LBRACK (statement semi)* (statement semi?)? RBRACK
  ;

returnExpression
  :  RETURN statement?
  ;

throwExpression
  : THROW statement?
  ;

jumpExpression
  :  breakExpression
  |  continueExpression
  ;

breakExpression
  : BREAK label?
  ;

continueExpression
  : CONTINUE label?
  ;

label
  : identifier
  ;

/* Operators */ 
multiplicativeOperator
  :  '*' | '/' | '%'
  ;

additiveOperator
  :  '+' | '-'
  ;

comparisonOperator
  :  '<' | '>' | '>=' | '<='
  ;

equalityOperator
  :  '!=' | '=='
  ;
 
assignmentOperator
  :  '=' | '+='
  ;

unaryOperator
  :  '!' | '++' | '--' | '+' | '-'
  ;

disjunctionOperator
  :  '||'
  ;

conjunctionOperator
  :  '&&'
  ;

// Type Hierarchy

simpleType
  :  identifier
  ;

literal
  :  IntegerLiteral
  |  BooleanLiteral
  |  CharacterLiteral
  |  StringLiteral
  |  NullLiteral
  ;

semi
  :  NL+
  |  SEMICOLON
  |  SEMICOLON NL+
  ;

// LEXER

identifier : Identifier;

// Keywords

CATCH : 'catch';
CLASS : 'class';
DO : 'do';
ELSE : 'else';
FINALLY : 'finally';
FOR : 'for';
FUN: 'fun';
IF : 'if';
MATCH : 'match';
NATIVE : 'native'; 
PRIVATE : 'private';
PUBLIC : 'public';
OVERRIDE: 'override';
RETURN : 'return';
THIS : 'this';
TRY : 'try';
VAL : 'val';
VAR : 'var';
WHILE : 'while';
BREAK : 'break';
CONTINUE: 'continue';
IMPORT: 'import';
PACKAGE: 'package';
THROW: 'throw';


// Integer Literals

IntegerLiteral
  :  DecimalIntegerLiteral
  |  HexadecimalIntegerLiteral
  |  OctalIntegerLiteral
  |  BinaryIntegerLiteral
  ;

fragment
DecimalIntegerLiteral
  :  DecimalNumeral
  ;

fragment
DecimalNumeral
  :  '0'
  |  NonZeroDigit Digit*
  ;

fragment
Digit
  :  '0'
  |  NonZeroDigit
  ;

fragment
NonZeroDigit
  :  [1-9]
  ;

fragment
HexadecimalIntegerLiteral
  :  HexadecimalPrefix HexadecimalDigit+
  ;

fragment
HexadecimalPrefix
  :  '0x'
  |  '0X'
  ;

fragment
HexadecimalDigit
  :  [0-9a-fA-F]
  ;

fragment
OctalIntegerLiteral
  :  OctalPrefix OctalDigit+
  ;

fragment
OctalPrefix
  :  '0'
  |  '0o'
  |  '0O'
  ;

fragment
OctalDigit
  :  [0-7]
  ;

fragment
BinaryIntegerLiteral
  :  BinaryPrefix BinaryDigit+
  ;

fragment
BinaryPrefix
  :  '0b'
  |  '0B'
  ;

fragment
BinaryDigit
  :  '0'
  |  '1'
  ;

// Boolean Literals

BooleanLiteral
  :  'true'
  |  'false'
  ;

// Character Literals

CharacterLiteral
  :  '\'' SingleCharacter '\''
  ;

fragment
SingleCharacter
  :  ~['\\\r\n]
  ;

// String Literals

StringLiteral
  :  '"' StringCharacters? '"'
  ;

fragment
StringCharacters
  :  StringCharacter+
  ;

fragment
StringCharacter
  :  ~["\\\r\n]
  ;

// The Null Literal

NullLiteral
  :  'null'
  ;

// Separators

ARROW : '->' ;
COMMA : ',' ;
DOT : '.' ;
ELLIPSIS : '...' ;
LBRACK : '[' ;
LCURLY : '{' ;
LPAREN : '(' ;
RBRACK : ']' ;
RCURLY : '}' ;
RPAREN : ')' ;
SEMICOLON :  ';' ;
AT : '@' ;

// Operators
GT : '>' ;
LT : '<' ;

// Identifiers 

Identifier
  :  SLangLetter SLangLetterOrDigit*
  ;

fragment
SLangLetter
  :  [a-zA-Z$_] 
  ;

fragment
SLangLetterOrDigit
  :  [a-zA-Z0-9$_] 
  ;

// Whitespace and comments 

WS  
  :  [ \t\r\n\u000C]+ -> skip
  ;

// COMMENTS
COMMENT
  :  '/*' .*? '*/' -> channel(1)
  ;

LINE_COMMENT
  :  '//' ~[\r\n]* -> channel(1)
  ;

NL
  :  '\u000D'? '\u000A'
  ;


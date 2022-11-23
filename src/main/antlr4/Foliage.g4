grammar Foliage;

program : (method)+ ;
method : type=Id ' '+ name=Id ' '* '{' statement+? '}' ;
statement : (intOperation | floatOperation | valueDeclaration | stackDeclaration | methodCall | externalMethodCall | instantiation | return) ' '* ';' ;
intOperation : ((integer | variable) operator)+ (integer | variable) ;
floatOperation : ((float | variable) operator)+ (float | variable) ;
valueDeclaration : type=Id ' '+ name=Id ' '* '=' ' '* val=value ;
stackDeclaration : type=Id ' '+ name=Id ' '* '=' ' '* pushesToStack ;
methodCall : name=Id '()' ;
externalMethodCall : klass=Id '.' name=Id '()' ;
instantiation : 'new ' klass=Id '()' ;
return : 'return ' val=value ;

operator : ('+' | '-' | '*' | '/') ;
integer : Number ;
float : Number | (Number 'f') | (Number '.' Number) ;
variable : Id ;
string : '"' Id '"' ;
value : integer | float | string ;
pushesToStack : intOperation | floatOperation | methodCall | externalMethodCall | instantiation ;

Id : IDEN+ ;
Number : DIGIT+ ;

fragment
IDEN : [A-Za-z_-] ;
DIGIT : [0-9] ;
WS : ('\t' | '\r' | '\n') -> skip ;
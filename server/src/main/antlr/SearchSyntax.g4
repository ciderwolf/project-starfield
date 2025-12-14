grammar SearchSyntax;

@header {
package starfield.search;
}

DASH: '-';
AND: 'and';
OR: 'or';
SEPERATOR: '=' | ':' | '>' | '>=' | '<=' | '<' | '!=';
WORD: [a-zA-Z0-9_*{}/]+;
QUOTED_STRING: '"' ( '\\' . | ~["\\])* '"';
REGEX: '/' ( '\\' . | ~[/\\])* '/';
LPAREN: '(';
RPAREN: ')';

WS: [ \t\r\n]+ -> skip;

search: rule (rule)*;
searchSubject: WORD | AND | OR | QUOTED_STRING;
rule:
	DASH? WORD SEPERATOR searchSubject #labelRule
	| LPAREN rule+ RPAREN #groupedRule
	| searchSubject #nameOnlyRule
	| rule AND rule #andRule
	| rule OR rule #orRule;
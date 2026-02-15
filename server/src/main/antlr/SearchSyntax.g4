grammar SearchSyntax;

@header {
package starfield.search;
}

DASH: '-';
AND: 'and';
OR: 'or';
SEPERATOR: '=' | ':' | '>' | '>=' | '<=' | '<' | '!=';
SEARCH_FILTER: [A-Za-z]+;
WORD: ~[ \t\r\n=:><!\-()"] ~[ \t\r\n=:><!()]*;
QUOTED_STRING: '"' ( '\\' . | ~["\\])* '"';
REGEX: '/' ( '\\' . | ~[/\\])* '/';
LPAREN: '(';
RPAREN: ')';

WS: [ \t\r\n]+ -> skip;

search: rule (rule)*;
searchSubject: WORD | SEARCH_FILTER | AND | OR | QUOTED_STRING;
rule:
	DASH? SEARCH_FILTER SEPERATOR searchSubject #labelRule
	| LPAREN rule+ RPAREN #groupedRule
	| searchSubject #nameOnlyRule
	| rule AND rule #andRule
	| rule OR rule #orRule;
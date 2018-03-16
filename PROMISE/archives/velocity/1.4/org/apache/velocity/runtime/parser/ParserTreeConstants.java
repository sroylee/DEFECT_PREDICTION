package org.apache.velocity.runtime.parser;

public interface ParserTreeConstants
{
  public int JJTPROCESS = 0;
  public int JJTVOID = 1;
  public int JJTESCAPEDDIRECTIVE = 2;
  public int JJTESCAPE = 3;
  public int JJTCOMMENT = 4;
  public int JJTNUMBERLITERAL = 5;
  public int JJTSTRINGLITERAL = 6;
  public int JJTIDENTIFIER = 7;
  public int JJTWORD = 8;
  public int JJTDIRECTIVE = 9;
  public int JJTBLOCK = 10;
  public int JJTOBJECTARRAY = 11;
  public int JJTINTEGERRANGE = 12;
  public int JJTMETHOD = 13;
  public int JJTREFERENCE = 14;
  public int JJTTRUE = 15;
  public int JJTFALSE = 16;
  public int JJTTEXT = 17;
  public int JJTIFSTATEMENT = 18;
  public int JJTELSESTATEMENT = 19;
  public int JJTELSEIFSTATEMENT = 20;
  public int JJTSETDIRECTIVE = 21;
  public int JJTEXPRESSION = 22;
  public int JJTASSIGNMENT = 23;
  public int JJTORNODE = 24;
  public int JJTANDNODE = 25;
  public int JJTEQNODE = 26;
  public int JJTNENODE = 27;
  public int JJTLTNODE = 28;
  public int JJTGTNODE = 29;
  public int JJTLENODE = 30;
  public int JJTGENODE = 31;
  public int JJTADDNODE = 32;
  public int JJTSUBTRACTNODE = 33;
  public int JJTMULNODE = 34;
  public int JJTDIVNODE = 35;
  public int JJTMODNODE = 36;
  public int JJTNOTNODE = 37;


  public String[] jjtNodeName = {
    "process",
    "void",
    "EscapedDirective",
    "Escape",
    "Comment",
    "NumberLiteral",
    "StringLiteral",
    "Identifier",
    "Word",
    "Directive",
    "Block",
    "ObjectArray",
    "IntegerRange",
    "Method",
    "Reference",
    "True",
    "False",
    "Text",
    "IfStatement",
    "ElseStatement",
    "ElseIfStatement",
    "SetDirective",
    "Expression",
    "Assignment",
    "OrNode",
    "AndNode",
    "EQNode",
    "NENode",
    "LTNode",
    "GTNode",
    "LENode",
    "GENode",
    "AddNode",
    "SubtractNode",
    "MulNode",
    "DivNode",
    "ModNode",
    "NotNode",
  };
}
package org.apache.lucene.analysis.standard;

public interface StandardTokenizerConstants {

  int EOF = 0;
  int ALPHANUM = 1;
  int APOSTROPHE = 2;
  int ACRONYM = 3;
  int COMPANY = 4;
  int EMAIL = 5;
  int HOST = 6;
  int NUM = 7;
  int P = 8;
  int HAS_DIGIT = 9;
  int ALPHA = 10;
  int LETTER = 11;
  int CJ = 12;
  int KOREAN = 13;
  int DIGIT = 14;
  int NOISE = 15;

  int DEFAULT = 0;

  String[] tokenImage = {
    "<EOF>",
    "<ALPHANUM>",
    "<APOSTROPHE>",
    "<ACRONYM>",
    "<COMPANY>",
    "<EMAIL>",
    "<HOST>",
    "<NUM>",
    "<P>",
    "<HAS_DIGIT>",
    "<ALPHA>",
    "<LETTER>",
    "<CJ>",
    "<KOREAN>",
    "<DIGIT>",
    "<NOISE>",
  };

}
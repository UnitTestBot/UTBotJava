/* Generated by: JavaCC 21 Parser Generator. InvalidToken.java */
package org.parsers.python;

/**
 * Token subclass to represent lexically invalid input
 */
public class InvalidToken extends Token {
    public InvalidToken(PythonLexer tokenSource, int beginOffset, int endOffset) {
        super(TokenType.INVALID, tokenSource, beginOffset, endOffset);
    }

    public String getNormalizedText() {
        return"Lexically Invalid Input:"+getImage();
    }

}

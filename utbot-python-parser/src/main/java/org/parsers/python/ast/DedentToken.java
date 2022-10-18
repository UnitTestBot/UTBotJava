/* Generated by: JavaCC 21 Parser Generator. Do not edit. 
  * Generated Code for DedentToken Token subclass
  * by the ASTToken.java.ftl template
  */
package org.parsers.python.ast;

import org.parsers.python.*;
import org.parsers.python.PythonConstants.TokenType;
import static org.parsers.python.PythonConstants.TokenType.*;
import java.util.List;
import java.util.ArrayList;
@SuppressWarnings("unused")
public class DedentToken extends Token {
    private List<Integer> indents;
    private int dedentAmount;
    public static Token makeDedentToken(Token followingToken, PythonLexer tokenSource, List<Integer> indents, int dedentAmount) {
        DedentToken result= new DedentToken(TokenType.DEDENT, tokenSource, 0, 0);
        result.indents= new ArrayList<Integer> (indents.size());
        result.indents.addAll(indents);
        result.dedentAmount= dedentAmount;
        followingToken.preInsert(result);
        return result;
    }

    public List<Integer> getIndents() {
        return new ArrayList<Integer> (indents);
    }

    public boolean isVirtual() {
        return true;
    }

    /*
 *  Commented out, as interferes with representation used in tests
 *
   public String toString() {
       return "DEDENT";
   }
 */
    public int getDedentAmount() {
        return dedentAmount;
    }

    public DedentToken(TokenType type, PythonLexer tokenSource, int beginOffset, int endOffset) {
        super(type, tokenSource, beginOffset, endOffset);
    }

}

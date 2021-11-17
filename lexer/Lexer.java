package lexer;

import java.util.Scanner;

/**
 * The Lexer class is responsible for scanning the source file
 * which is a stream of characters and returning a stream of
 * tokens; each token object will contain the string (or access
 * to the string) that describes the token along with an
 * indication of its location in the source program to be used
 * for error reporting; we are tracking line numbers; white spaces
 * are space, tab, newlines
 */
public class Lexer {
    private boolean atEOF = false;
    // next character to process
    private char ch;
    private SourceReader source;

    // positions in line of current token
    private int startPosition, endPosition,line;
    private boolean stringMark = false;
    private boolean charMark = false;

    /**
     * Lexer constructor
     *
     * @param sourceFile is the name of the File to read the program source from
     */
    public Lexer(String sourceFile) throws Exception {
        // init token table
        new TokenType();
        source = new SourceReader(sourceFile);
        ch = source.read();
    }

    /**
     * newIdTokens are either ids or reserved words; new id's will be inserted
     * in the symbol table with an indication that they are id's
     *
     * @param id            is the String just scanned - it's either an id or reserved word
     * @param startPosition is the column in the source file where the token begins
     * @param endPosition   is the column in the source file where the token ends
     * @return the Token; either an id or one for the reserved words
     */
    public Token newIdToken(String id, int startPosition, int endPosition) {
        return new Token(
                startPosition,
                endPosition,
                Symbol.symbol(id, Tokens.Identifier), source.getLineno()
        );
    }

    /**
     * number tokens are inserted in the symbol table; we don't convert the
     * numeric strings to numbers until we load the bytecodes for interpreting;
     * this ensures that any machine numeric dependencies are deferred
     * until we actually run the program; i.e. the numeric constraints of the
     * hardware used to compile the source program are not used
     *
     * @param number        is the int String just scanned
     * @param startPosition is the column in the source file where the int begins
     * @param endPosition   is the column in the source file where the int ends
     * @return the int Token
     */
    public Token newNumberToken(String number, int startPosition, int endPosition) {
        return new Token(
                startPosition,
                endPosition,
                Symbol.symbol(number, Tokens.INTeger),source.getLineno()
        );
    }

    public Token newStringToken(String string, int startPosition, int endPosition) {
        return new Token(
                startPosition,
                endPosition,
                Symbol.symbol(string, Tokens.StringLit),source.getLineno()
        );
    }

    public Token newCharToken(String string, int startPosition, int endPosition) {
        return new Token(
                startPosition,
                endPosition,
                Symbol.symbol(string, Tokens.CharLit),source.getLineno()
        );
    }

    /**
     * build the token for operators (+ -) or separators (parens, braces)
     * filter out comments which begin with two slashes
     *
     * @param s             is the String representing the token
     * @param startPosition is the column in the source file where the token begins
     * @param endPosition   is the column in the source file where the token ends
     * @return the Token just found
     */
    public Token makeToken(String s, int startPosition, int endPosition) {
        // filter comments

        if (s.equals("//")) {
            try {
                int oldLine = source.getLineno();

                do {
                    ch = source.read();
                } while (oldLine == source.getLineno());
            } catch (Exception e) {
                atEOF = true;
            }

            return nextToken();
        }


        if (s.equals("\'") && !charMark) {
            charMark = true;
            String tk = "\'";
            tk += ch;
            boolean valid=false;
            try {
                ch = source.read();
                while (true) {
                    if (ch == '\'') {
                        valid = true;
                        break;
                    }
                    endPosition++;
                    tk += ch;
                    ch = source.read();
                }
            } catch (Exception e) {
                atEOF = true;
            }

            tk+="\'";
            if (tk.length() == 3)
                return newCharToken(tk, startPosition, endPosition);
            else {
                System.out.println("******** illegal character: " + tk);
                atEOF = true;
                return nextToken();
            }
        }

        if (s.equals("\'") && charMark) {
            return nextToken();
        }


        if (s.equals("\"") && !stringMark) {
            stringMark = true;
            boolean valid = false;
            String tk = "\"";
            tk += ch;
            try {
                ch = source.read();
                while (true) {
                    if (ch == '\"') {
                        valid = true;
                        break;
                    }
                    tk += ch;
                    endPosition++;
                    //tk += ch;
                    ch = source.read();
                }
            } catch (Exception e) {
                atEOF = true;
            }

            if (valid) {
                tk+="\"";
                return newStringToken(tk, startPosition, endPosition);
            } else {
                System.out.println("******** illegal character: " + tk);
                atEOF = true;
                return nextToken();
            }
        }

        if (s.equals("\"") && stringMark) {
            return nextToken();
        }


        // ensure it's a valid token
        Symbol sym = Symbol.symbol(s, Tokens.BogusToken);

        if (sym == null) {
            System.out.println("******** illegal character: " + s);
            atEOF = true;
            return nextToken();
        }

        return new Token(startPosition, endPosition, sym,source.getLineno());
    }

    /**
     * @return the next Token found in the source file
     */
    public Token nextToken() {
        // ch is always the next char to process
        if (atEOF) {
            if (source != null) {
                source.close();
                source = null;
            }

            return null;
        }

        try {
            // scan past whitespace
            while (Character.isWhitespace(ch)) {
                ch = source.read();
            }
        } catch (Exception e) {
            atEOF = true;
            return nextToken();
        }

        startPosition = source.getPosition();
        endPosition = startPosition - 1;

        if (Character.isJavaIdentifierStart(ch)) {
            // return tokens for ids and reserved words
            String id = "";
            try {
                do {
                    endPosition++;
                    id += ch;
                    ch = source.read();
                } while (Character.isJavaIdentifierPart(ch));

            } catch (Exception e) {
                atEOF = true;
            }

            return newIdToken(id, startPosition, endPosition);
        }

        if (Character.isDigit(ch)) {
            // return number tokens
            String number = "";

            try {
                do {
                    endPosition++;
                    number += ch;
                    ch = source.read();
                } while (Character.isDigit(ch));
            } catch (Exception e) {
                atEOF = true;
            }

            return newNumberToken(number, startPosition, endPosition);
        }

        // At this point the only tokens to check for are one or two
        // characters; we must also check for comments that begin with
        // 2 slashes
        String charOld = "" + ch;
        String op = charOld;
        Symbol sym;
        try {
            endPosition++;
            ch = source.read();
            op += ch;

            // check if valid 2 char operator; if it's not in the symbol
            // table then don't insert it since we really have a one char
            // token
            sym = Symbol.symbol(op, Tokens.BogusToken);
            if (sym == null) {
                // it must be a one char token
                return makeToken(charOld, startPosition, endPosition);
            }

            endPosition++;
            ch = source.read();
            //System.out.println("making a token: "+op);
            return makeToken(op, startPosition, endPosition);
        } catch (Exception e) { /* no-op */ }

        atEOF = true;
        if (startPosition == endPosition) {
            op = charOld;
        }

        return makeToken(op, startPosition, endPosition);
    }


    public static void main(String args[]) {
        Token token;

        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter fileName: ");
            String fileName = scanner.next();
            Lexer lex = new Lexer("sample_files/"+fileName);

            while (true) {
                token = lex.nextToken();

                System.out.printf("%-10s left: %-7s right: %-7s line: %-7s %s\n", token.getSymbol(), token.getLeftPosition(), token.getRightPosition(), lex.source.getLineno(), token.getKind());
            }

        } catch (Exception e) {
            System.out.println("usage: java lexer.Lexer filename.x");
        }
    }

}
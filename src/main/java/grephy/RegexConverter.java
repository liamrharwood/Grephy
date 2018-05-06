package grephy;

import javax.xml.bind.ValidationException;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

/**
 * RegexConverter.java - Handles converting regex to NFA.
 */
public class RegexConverter {

    /**
     * Represents the different regex operators
     */
    private enum OPERATOR {
        CONCAT,
        UNION, // |
        PARENTHESES // (
    }

    /**
     * Creates an NFA that cycles an input NFA 0 or more times
     *
     * @param n NFA to repeat
     * @return The kleene closure of n
     */
    private static NFA kleeneStar(NFA n) {
        NFA result = new NFA(n.states.size() + 2);

        result.deltaE.add(new Transition(0, 1, Optional.empty()
        ));

        for (Transition transition : n.delta) {
            result.delta.add(new Transition(transition.stateFrom + 1,
                    transition.stateTo + 1,
                    transition.symbol));
        }
        for (Transition transition : n.deltaE) {
            result.deltaE.add(new Transition(transition.stateFrom + 1,
                    transition.stateTo + 1,
                    Optional.empty()));
        }

        result.deltaE.add(new Transition(n.states.size(), n.states.size() + 1, Optional.empty()));
        result.deltaE.add(new Transition(n.states.size(), 1, Optional.empty()));
        result.deltaE.add(new Transition(0, n.states.size() + 1, Optional.empty()));

        result.acceptingStates.clear();
        result.acceptingStates.add(result.states.size() - 1);

        return result;
    }

    /**
     * Creates a new NFA that is the concatenation of two input NFAs
     *
     * @param n The first NFA to concat
     * @param m The second NFA to concat
     * @return The concatenation of n and m
     */
    private static NFA concat(NFA n, NFA m) {
        m.states.remove(0);

        for (Transition transition : m.delta) {
            n.delta.add(new Transition(transition.stateFrom + n.states.size() - 1,
                    transition.stateTo + n.states.size() - 1,
                    transition.symbol));
        }
        for (Transition transition : m.deltaE) {
            n.deltaE.add(new Transition(transition.stateFrom + n.states.size() - 1,
                    transition.stateTo + n.states.size() - 1,
                    Optional.empty()));
        }

        for (Integer state : m.states) {
            n.states.add(state + n.states.size() + 1);
        }

        n.acceptingStates.clear();
        n.acceptingStates.add(n.states.size() - 1);

        return n;
    }

    /**
     * Creates a branching NFA that branches between two input NFAs
     *
     * @param n The first possible NFA to branch to
     * @param m The second possible NFA to branch to
     * @return The union of n and m
     */
    private static NFA union(NFA n, NFA m) {
        NFA result = new NFA(n.states.size() + m.states.size() + 2);

        result.deltaE.add(new Transition(0, 1, Optional.empty()));

        for (Transition transition : n.delta) {
            result.delta.add(new Transition(transition.stateFrom + 1,
                    transition.stateTo + 1,
                    transition.symbol));
        }
        for (Transition transition : n.deltaE) {
            result.deltaE.add(new Transition(transition.stateFrom + 1,
                    transition.stateTo + 1,
                    Optional.empty()));
        }

        result.deltaE.add(new Transition(n.states.size(), n.states.size() + m.states.size() + 1, Optional.empty()));
        result.deltaE.add(new Transition(0, n.states.size() + 1, Optional.empty()));

        for (Transition transition : m.delta) {
            result.delta.add(new Transition(transition.stateFrom + n.states.size() + 1,
                    transition.stateTo + n.states.size() + 1,
                    transition.symbol));
        }
        for (Transition transition : m.deltaE) {
            result.deltaE.add(new Transition(transition.stateFrom + n.states.size() + 1,
                    transition.stateTo + n.states.size() + 1,
                    Optional.empty()));
        }

        result.deltaE.add(new Transition(m.states.size() + n.states.size(),
                m.states.size() + n.states.size() + 1,
                Optional.empty()));

        result.acceptingStates.clear();
        result.acceptingStates.add(result.states.size() - 1);

        return result;
    }

    /**
     * Converts a regular expression string (in grep format) to an NFA.
     *
     * @param regex Regular expression string
     * @param alphabet Alphabet to be used in the NFA
     * @return The created NFA
     * @throws ValidationException if the regex is not formatted correctly
     */
    public static NFA nfaFromRegex(String regex, List<Character> alphabet) throws ValidationException {
        Stack<OPERATOR> operators = new Stack(); // Operators get added to the top as they are read and popped off when used
        Stack<NFA> operands = new Stack(); // Operand NFAs are added to the top and popped off when operators are used
        Stack<NFA> concats = new Stack(); // NFAs being concatenated together
        boolean shouldConcat = false; // Should the next operand be concatenated?
        boolean charEscaped = false; // Was backslash used to indicate an escaped character?
        char c; // Current character
        OPERATOR op; // Current operator
        int numParentheses = 0;
        NFA nfa1, nfa2;

        for (int i = 0; i < regex.length(); i++) {
            c = regex.charAt(i);
            if (c == '\\') {
                charEscaped = true;
                c = regex.charAt(++i);
            }

            if (alphabet.contains(c) || charEscaped) { // If not an operator, or an escaped character of any kind
                charEscaped = false;
                operands.push(new NFA(c));
                if (shouldConcat) {
                    operators.push(OPERATOR.CONCAT);
                } else {
                    shouldConcat = true;
                }
            } else {
                if (c == ')') {
                    shouldConcat = true;
                    if (numParentheses == 0) {
                        throw new ValidationException("Mismatched parentheses in regex.");
                    } else {
                        numParentheses--;
                    }

                    // Handle groupings of operators denoted by parentheses (work backwards until open paren)
                    while (!operators.empty() && operators.peek() != OPERATOR.PARENTHESES) {
                        op = operators.pop();
                        if (op == OPERATOR.CONCAT) {
                            nfa2 = operands.pop();
                            nfa1 = operands.pop();
                            operands.push(concat(nfa1, nfa2));
                        } else if (op == OPERATOR.UNION) {
                            nfa2 = operands.pop();

                            if (!operators.empty() && operators.peek() == OPERATOR.CONCAT) {
                                concats.push(operands.pop());
                                while (!operators.empty() && operators.peek() == OPERATOR.CONCAT) {
                                    concats.push(operands.pop());
                                    operators.pop();
                                }
                                nfa1 = concat(concats.pop(), concats.pop());
                                while (concats.size() > 0) {
                                    nfa1 = concat(nfa1, concats.pop());
                                }
                            } else {
                                nfa1 = operands.pop();
                            }
                            operands.push(union(nfa1, nfa2));
                        }
                    }
                } else if (c == '*') {
                    operands.push(kleeneStar(operands.pop()));
                    shouldConcat = true;
                } else if (c == '(') {
                    if (operands.size() > 0 && i < regex.length()-1
                            && regex.charAt(i + 1) != '(' && regex.charAt(i - 1) != '|') {
                        operators.push(OPERATOR.CONCAT);
                    }
                    operators.push(OPERATOR.PARENTHESES);
                    numParentheses++;
                    shouldConcat = false;
                } else if (c == '|') {
                    operators.push(OPERATOR.UNION);
                    shouldConcat = false;
                }
            }
        }

        // Go through remaining operators and perform operations as needed
        while (operators.size() > 0) {
            if (operands.size() < 2) {
                throw new ValidationException("Operator missing operand.");
            }
            op = operators.pop();
            if (op == OPERATOR.CONCAT) {
                nfa2 = operands.pop();
                nfa1 = operands.pop();
                operands.push(concat(nfa1, nfa2));
            } else if (op == OPERATOR.UNION) {
                nfa2 = operands.pop();

                if (!operators.empty() && operators.peek() == OPERATOR.CONCAT) {
                    concats.push(operands.pop());
                    while (!operators.empty() && operators.peek() == OPERATOR.CONCAT) {
                        concats.push(operands.pop());
                        operators.pop();
                    }
                    nfa1 = concat(concats.pop(), concats.pop());
                    while (concats.size() > 0) {
                        nfa1 = concat(nfa1, concats.pop());
                    }
                } else {
                    nfa1 = operands.pop();
                }
                operands.push(union(nfa1, nfa2));
            }
        }

        // The completed NFA will be at the top of the operand stack
        return operands.pop();
    }

}

package grephy;

import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class RegexConverter {

    private enum OPERATOR {
        CONCAT,
        KLEENE,
        UNION,
        PARENTHESES
    }

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

    public static NFA nfaFromRegex(String regex, List<Character> alphabet) {
        Stack<OPERATOR> operators = new Stack();
        Stack<NFA> operands = new Stack();
        Stack<NFA> concats = new Stack();
        boolean shouldConcat = false;
        boolean charEscaped = false;
        char c;
        OPERATOR op;
        int numParentheses = 0;
        NFA nfa1, nfa2;

        for (int i = 0; i < regex.length(); i++) {
            c = regex.charAt(i);
            if (c == '\\') {
                charEscaped = true;
                c = regex.charAt(++i);
            }

            if (alphabet.contains(c) || charEscaped) {
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
                        // error
                        System.exit(1);
                    } else {
                        numParentheses--;
                    }

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
                    if (operands.size() > 0 && i < regex.length()-1 && regex.charAt(i + 1) != '(') {
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

        while (operators.size() > 0) {
            if (operands.empty()) {
                //error
                System.exit(1);
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

        return operands.pop();
    }

}

package grephy;

import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class RegexConverter {

    private static NFA kleeneStar(NFA n) {
        NFA result = new NFA(n.states.size() + 2);

        result.deltaE.add(new Transition(0, 1, null));

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

        result.acceptingState = result.states.size() - 1;
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

        n.acceptingState = n.states.size() - 1;
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

        result.acceptingState = result.states.size() - 1;
        return result;
    }

    public static NFA nfaFromRegex(String regex, List<Character> alphabet) {
        Stack<Character> operators = new Stack();
        Stack<NFA> operands = new Stack();
        Stack<NFA> concats = new Stack();
        boolean shouldConcat = false;
        char op, c;
        int numParentheses = 0;
        NFA nfa1, nfa2;

        for (int i = 0; i < regex.length(); i++) {
            c = regex.charAt(i);
            if (alphabet.contains(c)) {
                operands.push(new NFA(c));
                if (shouldConcat) {
                    operators.push('.');
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

                    while (!operators.empty() && operators.peek() != '(') {
                        op = operators.pop();
                        if (op == '.') {
                            nfa2 = operands.pop();
                            nfa1 = operands.pop();
                            operands.push(concat(nfa1, nfa2));
                        } else if (op == '|') {
                            nfa2 = operands.pop();

                            if (!operators.empty() && operators.peek() == '.') {
                                concats.push(operands.pop());
                                while (!operators.empty() && operators.peek() == '.') {
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
                    if (operands.size() > 0) {
                        operators.push('.');
                    }
                    operators.push(c);
                    numParentheses++;
                    shouldConcat = false;
                } else if (c == '|') {
                    operators.push(c);
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
            if (op == '.') {
                nfa2 = operands.pop();
                nfa1 = operands.pop();
                operands.push(concat(nfa1, nfa2));
            } else if (op == '|') {
                nfa2 = operands.pop();

                if (!operators.empty() && operators.peek() == '.') {
                    concats.push(operands.pop());
                    while (!operators.empty() && operators.peek() == '.') {
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

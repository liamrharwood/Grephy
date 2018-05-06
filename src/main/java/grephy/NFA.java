package grephy;

import java.util.*;
import java.util.stream.Collectors;

/**
 * NFA.java - Represents a nondeterministic finite automata.
 */
public class NFA {

    public ArrayList<Integer> states = new ArrayList(); // States are identified by their INDEX in this list
    public ArrayList<Transition> delta = new ArrayList(); // State transitions on symbols
    public ArrayList<Transition> deltaE = new ArrayList(); // State transitions on epsilon (empty string)
    public ArrayList<Integer> acceptingStates = new ArrayList();

    private final int INITIAL_STATE = 0; // The first index of states is always the initial state (for simplicity)

    /**
     * Constructs an NFA with a specified number of states and no transitions.
     *
     * @param numStates Number of states to generate
     */
    public NFA(int numStates) {
        this.acceptingStates.add(INITIAL_STATE); // Make the initial state an accepting state

        for (int i = 0; i < numStates; i++) {
            this.states.add(i);
        }
    }

    /**
     * Constructs an NFA with a single transition on a specified character.
     *
     * @param c The character to transition on
     */
    public NFA(char c) {
        this.acceptingStates.add(INITIAL_STATE + 1); // Accept on the second state

        // Create two states and a transition between them on c
        for (int i = 0; i < 2; i++) {
            this.states.add(i);
        }

        this.delta.add(new Transition(INITIAL_STATE, INITIAL_STATE + 1, Optional.of(c)));
    }

    /**
     * Default constructor. Necessary for DFA subclass.
     */
    public NFA() {
        this.acceptingStates.add(INITIAL_STATE);
    }

    /**
     * Determines if a string is accepted or rejected by the NFA.
     *
     * @param state Starting state
     * @param inputStr String being processed
     * @param pos Current position in the string
     * @return true if accepted, false if rejected
     */
    public boolean accepts(int state, String inputStr, int pos) {
        // At the end of the string, check if epsilon goes anywhere, if not, check if in accepting state
        if (pos == inputStr.length()) {
            List<Transition> transitions = new ArrayList(deltaE);
            transitions.removeIf(t -> t.stateFrom != state);
            for (int i = 0; i < transitions.size(); i++) {
                if (accepts(transitions.get(i).stateTo, inputStr, pos))
                    return true;
            }
            return acceptingStates.contains(state);
        }

        char c = inputStr.charAt(pos);
        List<Transition> transitions = new ArrayList(delta);
        transitions.removeIf(t -> t.stateFrom != state || t.symbol.get() != c); // Get transitions from current state on c

        // Recursively check transitions on current character to see if they lead to accepting state
        for (int i = 0; i < transitions.size(); i++) {
            if (accepts(transitions.get(i).stateTo, inputStr, pos+1))
                return true;
        }

        transitions = new ArrayList(deltaE);
        transitions.removeIf(t -> t.stateFrom != state); // Get epsilon transitions from current state
        // Recursively check transitions on empty string to see if they lead to accepting state
        for (int i = 0; i < transitions.size(); i++) {
            if (accepts(transitions.get(i).stateTo, inputStr, pos))
                return true;
        }

        // If nothing has accepted, reject the string
        return false;
    }

    /**
     * Determines if a string is accepted or rejected by the NFA, starting from the initial state and beginning of
     * the string.
     *
     * @param inputStr String to be processed
     * @return true if accepted, false if rejected
     */
    public boolean accepts(String inputStr) {
        return accepts (INITIAL_STATE, inputStr, 0);
    }

    /**
     * Convert the NFA to the DOT language file format.
     *
     * @return A list of lines in the DOT file
     */
    public List<String> toDotFile() {
        List<String> result = new ArrayList();
        result.add("digraph G {");
        result.add("node [shape=circle];");
        result.add("start [shape=none  label=\"\"]"); // Blank node from which initial arrow comes

        // Make accepting states into double circles (if reachable)
        for (Integer state : acceptingStates) {
            ArrayList<Transition> ts = new ArrayList(delta);
            ts.removeIf(t -> t.stateTo != state);
            if (state == INITIAL_STATE || !ts.isEmpty()) {
                result.add(state + " [shape=doublecircle];");
            }
        }
        result.add("start -> " + INITIAL_STATE + ";"); // Initial arrow
        // Create transitions on symbols
        for (Transition t : delta) {
            String s;
            if (t.symbol.get() == '\\') { // Avoid DOT escape character weirdness
                s = t.stateFrom + " -> " + t.stateTo + " [label=\"\\\\\"];";
            } else {
                s = t.stateFrom + " -> " + t.stateTo + " [label=\"" + t.symbol.get() + "\"];";
            }
            result.add(s);
        }
        // Create transitions on empty string
        for (Transition t : deltaE) {
            result.add(t.stateFrom + " -> " + t.stateTo + " [label=\"&epsilon;\"];");
        }
        result.add("}");

        return result;
    }

    /**
     * Remove epsilon transitions from the NFA to make it easier to convert to DFA.
     */
    public void removeEpsilons() {
        ArrayList<Integer> oldAcceptingStates = new ArrayList(acceptingStates);

        // Use epsilon closure to find accepting states and add new symbol transitions where epsilons aren't needed
        for (int i = 0; i < states.size(); i++) {
            ArrayList<Integer> eClose = findEClose(i, new ArrayList<Integer>());
            for (Integer state : oldAcceptingStates) {
                if (eClose.contains(state)) {
                    acceptingStates.add(i);
                    break;
                }
            }

            for (int j = 0; j < states.size(); j++) {
                for (Integer state : eClose) {
                    ArrayList<Transition> ts = new ArrayList(delta);
                    int index = j;
                    ts.removeIf(t -> t.stateFrom != state || t.stateTo != index);
                    for (Transition t : ts) {
                        delta.add(new Transition(i, j, t.symbol));
                    }
                }
            }
        }

        // Remove transitions from unreachable states
        for (int i = 1; i < states.size(); i++) {
            ArrayList<Transition> ts = new ArrayList(delta);
            int index = i;
            if (ts.stream().filter(t -> t.stateTo == index).collect(Collectors.toList()).isEmpty()) {
                delta.removeIf(t -> t.stateFrom == index);
            }
        }

        // Get rid of the epsilon transitions
        deltaE.clear();
    }

    /**
     * Determines the epsilon closure of a given state.
     *
     * @param state State being examined
     * @param states States in the epsilon closure (so far)
     * @return Full epsilon closure
     */
    private ArrayList<Integer> findEClose(int state, ArrayList<Integer> states) {
        List<Transition> transitions = new ArrayList(deltaE);
        transitions.removeIf(t -> t.stateFrom != state);

        for (Transition t : transitions) {
            if (!states.contains(t.stateTo))
                states.add(t.stateTo);
            states = findEClose(t.stateTo, states);
        }

        return states;
    }
}

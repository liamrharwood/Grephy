package grephy;

import java.util.*;

public class NFA {
    public ArrayList<Integer> states;
    public ArrayList<Transition> delta;
    public ArrayList<Transition> deltaE;
    public int acceptingState;

    public NFA(int numStates) {
        this.states = new ArrayList();
        this.delta = new ArrayList();
        this.deltaE = new ArrayList();
        this.acceptingState = 0;

        for (int i = 0; i < numStates; i++) {
            this.states.add(i);
        }
    }

    public NFA(char c) {
        this.states = new ArrayList();
        this.delta = new ArrayList();
        this.deltaE = new ArrayList();
        this.acceptingState = 1;

        for (int i = 0; i < 2; i++) {
            this.states.add(i);
        }

        this.delta.add(new Transition(0, 1, Optional.of(c)));
    }

    public boolean accepts(int state, String inputStr, int pos) {
        if (pos == inputStr.length()) {
            List<Transition> transitions = new ArrayList(deltaE);
            transitions.removeIf(t -> t.stateFrom != state);
            for (int i = 0; i < transitions.size(); i++) {
                if (accepts(transitions.get(i).stateTo, inputStr, pos))
                    return true;
            }
            return state == acceptingState;
        }

        char c = inputStr.charAt(pos);
        List<Transition> transitions = new ArrayList(delta);
        transitions.removeIf(t -> t.stateFrom != state || t.symbol.get() != c);

        for (int i = 0; i < transitions.size(); i++) {
            if (accepts(transitions.get(i).stateTo, inputStr, pos+1))
                return true;
        }

        transitions = new ArrayList(deltaE);
        transitions.removeIf(t -> t.stateFrom != state);
        for (int i = 0; i < transitions.size(); i++) {
            if (accepts(transitions.get(i).stateTo, inputStr, pos))
                return true;
        }

        return false;
    }

    public boolean accepts(String inputStr) {
        return accepts (0, inputStr, 0);
    }

    public List<String> toDotFile() {
        List<String> result = new ArrayList();
        result.add("digraph G {");
        for (Transition t : delta) {
            result.add(t.stateFrom + " -> " + t.stateTo + " [label=" + t.symbol.get() + "];");
        }
        for (Transition t : deltaE) {
            result.add(t.stateFrom + " -> " + t.stateTo + " [label=\"&epsilon;\"];");
        }
        result.add("}");

        return result;
    }
}

package grephy;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class NFA {
    public ArrayList<Integer> states = new ArrayList();
    public ArrayList<Transition> delta = new ArrayList();
    public ArrayList<Transition> deltaE = new ArrayList();
    public ArrayList<Integer> acceptingStates = new ArrayList();

    public NFA(int numStates) {
        this.acceptingStates.add(0);

        for (int i = 0; i < numStates; i++) {
            this.states.add(i);
        }
    }

    public NFA(char c) {
        this.acceptingStates.add(1);

        for (int i = 0; i < 2; i++) {
            this.states.add(i);
        }

        this.delta.add(new Transition(0, 1, Optional.of(c)));
    }

    public NFA() {
        this.acceptingStates.add(0);
    }

    public boolean accepts(int state, String inputStr, int pos) {
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
        result.add("node [shape=circle];");
        result.add("start [shape=none  label=\"\"]");
        for (Integer state : acceptingStates) {
            result.add(state + " [shape=doublecircle];");
        }
        result.add("start -> 0;");
        for (Transition t : delta) {
            String s = t.stateFrom + " -> " + t.stateTo + " [label=" + t.symbol.get() + "];";
            result.add(s);
        }
        for (Transition t : deltaE) {
            result.add(t.stateFrom + " -> " + t.stateTo + " [label=\"&epsilon;\"];");
        }
        result.add("}");

        return result;
    }

    public void removeEpsilons() {
        ArrayList<Integer> oldAcceptingStates = new ArrayList(acceptingStates);
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

        for (int i = 1; i < states.size(); i++) {
            ArrayList<Transition> ts = new ArrayList(delta);
            int index = i;
            if (ts.stream().filter(t -> t.stateTo == index).collect(Collectors.toList()).isEmpty()) {
                delta.removeIf(t -> t.stateFrom == index);
            }
        }

        deltaE.clear();
    }

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

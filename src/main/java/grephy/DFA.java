package grephy;

import java.lang.reflect.Array;
import java.util.*;

public class DFA extends NFA {
    ArrayList<Set<Integer>> stateSubsets = new ArrayList();

    public DFA (NFA nfa, List<Character> alphabet) {
        // Create initial state
        states.add(0);
        // Create subset for initial state (only contains initial NFA state)
        stateSubsets.add(new HashSet());
        stateSubsets.get(0).add(0);

        int i = 0;
        while (i < states.size()) {
            for (Character c : alphabet) {
                List<Transition> cTransitions = new ArrayList(nfa.delta);
                int index = i;
                cTransitions.removeIf(t -> !stateSubsets.get(index).contains(t.stateFrom)
                        || t.symbol.get() != c);
                HashSet<Integer> toStates = new HashSet();
                for (Transition transition : cTransitions) {
                    toStates.add(transition.stateTo);
                }

                boolean flag = false;
                for (int j = 0; j < states.size(); j++) {
                    if (toStates.containsAll(stateSubsets.get(j)) && stateSubsets.get(j).containsAll(toStates)) {
                        delta.add(new Transition(i, j, Optional.of(c)));
                        flag = true;
                        break;
                    }
                }

                if (!flag) {
                    states.add(states.size() - 1);
                    delta.add(new Transition(i, states.size() - 1, Optional.of(c)));
                    stateSubsets.add(new HashSet(toStates));
                }
            }

            i++;
        }

        acceptingStates.clear();
        for (i = 0; i < states.size(); i++) {
            for (Integer state : nfa.acceptingStates) {
                if (stateSubsets.get(i).contains(state)) {
                    acceptingStates.add(i);
                    break;
                }
            }
        }

        minimize(alphabet);
    }

    private void minimize(List<Character> alphabet) {
        ArrayList<ArrayList<Integer>> partitions = new ArrayList();
        partitions.add(new ArrayList(acceptingStates));
        partitions.add(new ArrayList());
        for (int i = 0; i < states.size(); i++) {
            if (!acceptingStates.contains(i)) {
                partitions.get(1).add(i);
            }
        }

        ArrayList<ArrayList<Integer>> waiting = new ArrayList();
        waiting.add(new ArrayList(acceptingStates));
        while (!waiting.isEmpty()) {
            ArrayList<Integer> A = waiting.remove(0);
            for (Character c : alphabet) {
                //let X be the set of states for which a transition on c leads to a state in A
                HashSet<Integer> X = new HashSet();
                for (Integer state : states) {
                    ArrayList<Transition> ts = new ArrayList(delta);
                    ts.removeIf(t -> t.symbol.get() != c || !A.contains(t.stateTo));
                    for (Transition t : ts) {
                        X.add(t.stateFrom);
                    }
                }

                int i = 0;
                while (i < partitions.size()) {
                    ArrayList<Integer> Y = new ArrayList(partitions.get(i));
                    ArrayList<Integer> X2 = new ArrayList(X);
                    X2.retainAll(Y);
                    ArrayList<Integer> Y2 = new ArrayList(Y);
                    Y2.removeAll(X);
                    if (!X2.isEmpty() && !Y2.isEmpty()) {
                        partitions.remove(i);
                        partitions.add(X2);
                        partitions.add(Y2);

                        boolean flag = false;
                        int j = 0;
                        while (j < waiting.size()) {
                            HashSet<Integer> set = new HashSet(waiting.get(j));
                            if (set.containsAll(Y) && Y.containsAll(set)) {
                                waiting.remove(j);
                                waiting.add(X2);
                                waiting.add(Y2);
                                flag = true;
                                break;
                            }
                            j++;
                        }
                        if (!flag) {
                            if (X2.size() <= Y2.size()) {
                                waiting.add(X2);
                            } else {
                                waiting.add(Y2);
                            }
                        }
                    }

                    i++;
                }
            }
        }
        mergeStates(partitions, alphabet);
    }

    private void mergeStates(List<ArrayList<Integer>> partitions, List<Character> alphabet) {
        ArrayList<Integer> newStates = new ArrayList();
        ArrayList<Transition> newDelta = new ArrayList();
        ArrayList<Integer> newAcceptingStates = new ArrayList();

        int i = 0;
        while (i < partitions.size()) {
            if (partitions.get(i).contains(0)) {
                partitions.add(0, partitions.get(i));
                partitions.remove(i+1);
                break;
            }
            i++;
        }

        for (i = 0; i < partitions.size(); i++) {
            for (int j = 0; j < partitions.get(i).size(); j++) {
                if (acceptingStates.contains(partitions.get(i).get(j))) {
                    newAcceptingStates.add(i);
                    break;
                }
            }
            newStates.add(i);
        }

        for (i = 0; i < newStates.size(); i++) {
            Integer s = partitions.get(i).get(0);
            for (Character c : alphabet) {
                ArrayList<Transition> transitions = new ArrayList(delta);
                transitions.removeIf(t -> t.stateFrom != s || t.symbol.get() != c);
                Transition t = transitions.get(0);
                int j = 0;
                while (j < partitions.size()) {
                    if (partitions.get(j).contains(t.stateTo)) {
                        newDelta.add(new Transition(i, j, t.symbol));
                        break;
                    }
                    j++;
                }
            }

        }

        states = newStates;
        delta = newDelta;
        acceptingStates = newAcceptingStates;
    }
}

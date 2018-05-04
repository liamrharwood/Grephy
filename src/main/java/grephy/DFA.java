package grephy;

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
                    states.add(i + 1);
                    stateSubsets.add(new HashSet(toStates));
                }
            }

            List<Transition> eTransitions = new ArrayList(nfa.deltaE);
            int index = i;
            eTransitions.removeIf(t -> !stateSubsets.get(index).contains(t.stateFrom));
            HashSet<Integer> toStates = new HashSet();
            for (Transition transition : eTransitions) {
                toStates.add(transition.stateTo);
            }

            boolean flag = false;
            for (int j = 0; j < states.size(); j++) {
                if (toStates.containsAll(stateSubsets.get(j)) && stateSubsets.get(j).containsAll(toStates)) {
                    delta.add(new Transition(i, j, Optional.empty()));
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                states.add(i+1);
                stateSubsets.add(new HashSet(toStates));
            }

            i++;
        }
    }
}

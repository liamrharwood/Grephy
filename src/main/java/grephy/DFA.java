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
    }
}

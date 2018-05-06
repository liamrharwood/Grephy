package grephy;

import java.util.*;

/**
 * DFA.java - Represents a deterministic finite automaton.
 */
public class DFA extends NFA {
    // Subsets for each state during subset construction; indices correspond to state list indices
    ArrayList<Set<Integer>> stateSubsets = new ArrayList();

    /**
     * Constructs a DFA from a given NFA and alphabet using subset construction.
     *
     * @param nfa
     * @param alphabet
     */
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
                        || t.symbol.get() != c); // Get transitions coming from a state in current subset on c
                HashSet<Integer> toStates = new HashSet();
                // Determine subset of possible next states
                for (Transition transition : cTransitions) {
                    toStates.add(transition.stateTo);
                }

                boolean flag = false;
                for (int j = 0; j < states.size(); j++) {
                    // If the subsets match, create a transition between them
                    if (toStates.containsAll(stateSubsets.get(j)) && stateSubsets.get(j).containsAll(toStates)) {
                        delta.add(new Transition(i, j, Optional.of(c)));
                        flag = true;
                        break;
                    }
                }

                // If no match was found, create a new state using the subset of possible next states
                if (!flag) {
                    states.add(states.size() - 1);
                    delta.add(new Transition(i, states.size() - 1, Optional.of(c)));
                    stateSubsets.add(new HashSet(toStates));
                }
            }

            i++;
        }

        acceptingStates.clear();
        // Determine the new accepting states from subsets containing NFA accepting states
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

    /**
     * Minimizes the DFA using Hopcroft's algorithm (https://en.wikipedia.org/wiki/DFA_minimization#Hopcroft's_algorithm)
     *
     * @param alphabet DFA alphabet
     */
    private void minimize(List<Character> alphabet) {
        ArrayList<ArrayList<Integer>> partitions = new ArrayList(); // Equivalence classes (called P in Hopcroft)
        partitions.add(new ArrayList(acceptingStates));
        partitions.add(new ArrayList());
        for (int i = 0; i < states.size(); i++) {
            if (!acceptingStates.contains(i)) {
                partitions.get(1).add(i);
            }
        }

        ArrayList<ArrayList<Integer>> waiting = new ArrayList(); // Partitions to examine (called W in Hopcroft)
        waiting.add(new ArrayList(acceptingStates));
        while (!waiting.isEmpty()) {
            ArrayList<Integer> A = waiting.remove(0); // Choose set from W
            for (Character c : alphabet) {
                HashSet<Integer> X = new HashSet(); // Set of states for which a transition on c leads to a state in A
                ArrayList<Transition> ts = new ArrayList(delta);
                ts.removeIf(t -> t.symbol.get() != c || !A.contains(t.stateTo));
                for (Transition t : ts) {
                    X.add(t.stateFrom);
                }


                int i = 0;
                while (i < partitions.size()) {
                    ArrayList<Integer> Y = new ArrayList(partitions.get(i));
                    ArrayList<Integer> xUnionY = new ArrayList(X); // X U Y
                    xUnionY.retainAll(Y);
                    ArrayList<Integer> ySubtractX = new ArrayList(Y); // X \ Y
                    ySubtractX.removeAll(X);
                    if (!xUnionY.isEmpty() && !ySubtractX.isEmpty()) {
                        partitions.remove(i); // Replace Y in P by the two sets X ∩ Y and Y \ X
                        partitions.add(xUnionY);
                        partitions.add(ySubtractX);

                        boolean flag = false;
                        int j = 0;
                        while (j < waiting.size()) {
                            HashSet<Integer> set = new HashSet(waiting.get(j));
                            if (set.containsAll(Y) && Y.containsAll(set)) { // If Y is in W
                                waiting.remove(j); // Replace Y in W by the two sets X ∩ Y and Y \ X
                                waiting.add(xUnionY);
                                waiting.add(ySubtractX);
                                flag = true;
                                break;
                            }
                            j++;
                        }
                        // If Y is not in W
                        if (!flag) {
                            if (xUnionY.size() <= ySubtractX.size()) {
                                waiting.add(xUnionY);
                            } else {
                                waiting.add(ySubtractX);
                            }
                        }
                    }

                    i++;
                }
            }
        }
        mergeStates(partitions, alphabet);
    }

    /**
     * Turns equivalence classes found by Hopcroft's algorithms into new states to finalize minimization
     *
     * @param partitions Equivalence classes found by Hopcroft's algorithm
     * @param alphabet DFA alphabet
     */
    private void mergeStates(List<ArrayList<Integer>> partitions, List<Character> alphabet) {
        ArrayList<Integer> newStates = new ArrayList();
        ArrayList<Transition> newDelta = new ArrayList();
        ArrayList<Integer> newAcceptingStates = new ArrayList();

        // Move the partition containing the initial state to the first index
        int i = 0;
        while (i < partitions.size()) {
            if (partitions.get(i).contains(INITIAL_STATE)) {
                partitions.add(INITIAL_STATE, partitions.get(i));
                partitions.remove(i+1);
                break;
            }
            i++;
        }

        // Determine which partitions contain accepting states and create new corresponding accepting states
        for (i = 0; i < partitions.size(); i++) {
            for (int j = 0; j < partitions.get(i).size(); j++) {
                if (acceptingStates.contains(partitions.get(i).get(j))) {
                    newAcceptingStates.add(i);
                    break;
                }
            }
            newStates.add(i);
        }

        // Create transitions between partitions that have transitions between the states they contain
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

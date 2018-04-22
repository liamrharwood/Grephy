package grephy;

import java.util.*;

public class NFA {
    public ArrayList<Integer> states;
    public ArrayList<Transition> delta;
    public ArrayList<Transition> deltaE;

    public NFA(int numStates) {
        this.states = new ArrayList();
        this.delta = new ArrayList();
        this.deltaE = new ArrayList();

        for (int i = 0; i < numStates; i++) {
            this.states.add(i);
        }
    }

    public NFA(char c) {
        this.states = new ArrayList();
        this.delta = new ArrayList();
        this.deltaE = new ArrayList();

        for (int i = 0; i < 2; i++) {
            this.states.add(i);
        }

        this.delta.add(new Transition(0, 1, c));
    }
}

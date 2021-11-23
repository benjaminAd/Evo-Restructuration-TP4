package main;

import testers.AProxy;

public interface ICallGraphs {
    static void doStuff() {
        new AProxy().foo();
    }

    void doOtherStuff();
}
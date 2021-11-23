package main;

import testers.CallGraphs;

public class CallGraphsProxy implements ICallGraphs {
        CallGraphs callgraph = new CallGraphs();

        public void doOtherStuff(){
            callgraph.doSmtDifferent();
        }
}
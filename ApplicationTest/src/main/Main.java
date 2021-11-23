package main;

public class Main {

    private static final ICallGraphs callGraphsproxy = new CallGraphsProxy();

    public static void main(String[] args) {
        ICallGraphs.doStuff();
        callGraphsproxy.doOtherStuff();
    }
}

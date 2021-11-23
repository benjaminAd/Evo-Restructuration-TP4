package testers;

public interface ICallGraphs {
    static void doSmt() {
        new AProxy().foo();
    }

    void doSmtDifferent();
}
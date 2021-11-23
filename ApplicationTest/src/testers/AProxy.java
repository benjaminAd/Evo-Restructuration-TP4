package testers;

import otherTest.A;

public class AProxy implements IA {
    A classicA = new A();

    @Override
    public void foo() {
        classicA.foo();
    }

    @Override
    public void bar() {
        classicA.bar();
    }
}

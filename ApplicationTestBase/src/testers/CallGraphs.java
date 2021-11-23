package testers;
public class CallGraphs
{
       public static void main(String[] args) {
              doStuff();
              doOtherStuff();
       }
       
       public static void doStuff() {
              new A().foo();
       }
       
       private static void doOtherStuff() {
    	   new A().bar();
       }
}

class A
{
       public void foo() {
              bar();
       }
       
       public void bar() {
       }
}
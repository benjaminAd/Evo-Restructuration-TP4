package testers;
import otherTest.A;

public class CallGraphs implements ICallGraphs
{
       
       public void doSmtDifferent() {
    	   new A().bar();
       }
}
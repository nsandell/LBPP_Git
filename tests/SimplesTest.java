package tests;

import bn.impl.staticbn.StaticNetworkFactory;
import bn.messages.FiniteDiscreteMessage;
import bn.statc.IFDiscBNNode;
import bn.statc.IStaticBayesNet;
import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;

public class SimplesTest {

	public static void main(String[] args) {
		try
		{
			double[][] cpt_values = {{.9, .1},{.25, .75}};
			IStaticBayesNet bn = StaticNetworkFactory.getNetwork();
			//IBayesNode root = bn.addDiscreteNode("root", 2);
			IFDiscBNNode root = bn.addDiscreteNode("root", 2);
			bn.setDistribution("root", new DiscreteCPTUC(new double[]{.7, .3}));
			IFDiscBNNode child = bn.addDiscreteNode("child", 2);
			bn.setDistribution("child",new DiscreteCPT(new int[]{2}, 2,cpt_values));
		
			bn.addEdge("root", "child");
			
			IFDiscBNNode child2 = bn.addDiscreteNode("child2", 2);
			bn.setDistribution("child2", new DiscreteCPT(new int[]{2}, 2,cpt_values));
			
			bn.addEdge("child", "child2");
			
			child.setValue(1);
			bn.validate();
			bn.run(20,0);
			
			FiniteDiscreteMessage rootmarg = root.getMarginal();
			FiniteDiscreteMessage childmarg = child.getMarginal();
			FiniteDiscreteMessage child2marg = child2.getMarginal();
			System.out.println("Root node probability : " + rootmarg.getValue(0) + "," + rootmarg.getValue(1));
			System.out.println("Child node probability : " + childmarg.getValue(0) + "," + childmarg.getValue(1));
			System.out.println("Child2 node probability : " + child2marg.getValue(0) + "," + child2marg.getValue(1));
			
		}
		catch(BNException e)
		{
			System.out.println("Error occurred while executing test : " + e.toString());
		}
	}

}

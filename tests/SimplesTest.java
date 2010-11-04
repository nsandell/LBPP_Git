package tests;

import bn.impl.BayesNetworkFactory;
import bn.messages.DiscreteMessage;
import bn.BNException;
import bn.IStaticBayesNet;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;

public class SimplesTest {

	public static void main(String[] args) {
		try
		{
			double[][] cpt_values = {{.9, .1},{.25, .75}};
			IStaticBayesNet bn = BayesNetworkFactory.getStaticNetwork();
			//IBayesNode root = bn.addDiscreteNode("root", 2);
			bn.addDiscreteNode("root", 2);
			bn.setDistribution("root", new DiscreteCPTUC(new double[]{.7, .3}));
			bn.addDiscreteNode("child", 2);
			bn.setDistribution("child",new DiscreteCPT(new int[]{2}, 2,cpt_values));
		
			bn.addEdge("root", "child");
			
			bn.addDiscreteNode("child2", 2);
			bn.setDistribution("child2", new DiscreteCPT(new int[]{2}, 2,cpt_values));
			
			bn.addEdge("child", "child2");
			
			bn.addEvidence("child", 1);
			bn.validate();
			bn.run(20,0);
			
			DiscreteMessage rootmarg =(DiscreteMessage)bn.getMarginal("root");
			DiscreteMessage childmarg =(DiscreteMessage)bn.getMarginal("child");
			DiscreteMessage child2marg =(DiscreteMessage)bn.getMarginal("child2");
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

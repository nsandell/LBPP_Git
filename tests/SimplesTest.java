package tests;

import bn.impl.BayesNetworkFactory;
import bn.BNException;
import bn.IDiscreteBayesNode;
import bn.IStaticBayesNet;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;

public class SimplesTest {

	public static void main(String[] args) {
		try
		{
			double[][] cpt_values = {{.9, .1},{.25, .75}};
			IStaticBayesNet bn = BayesNetworkFactory.getStaticNetwork();
			IDiscreteBayesNode root = bn.addDiscreteNode("root", 2);
			root.setDistribution(new DiscreteCPTUC(new double[]{.7, .3}));
			IDiscreteBayesNode child = bn.addDiscreteNode("child", 2);
			child.setDistribution(new DiscreteCPT(new int[]{2}, 2,cpt_values));
		
			bn.addEdge(root, child);
			
			IDiscreteBayesNode child2 = bn.addDiscreteNode("child2", 2);
			child2.setDistribution(new DiscreteCPT(new int[]{2}, 2,cpt_values));
			
			bn.addEdge("child", "child2");
			
			child2.setValue(1);
			bn.validate();
			bn.run(20,0);
			
			System.out.println("Root node probability : " + root.getMarginal().getValue(0) + "," + root.getMarginal().getValue(1));
			System.out.println("Child node probability : " + child.getMarginal().getValue(0) + "," + child.getMarginal().getValue(1));
			System.out.println("Child2 node probability : " + child2.getMarginal().getValue(0) + "," + child2.getMarginal().getValue(1));
			
		}
		catch(BNException e)
		{
			System.out.println("Error occurred while executing test : " + e.toString());
		}
	}

}

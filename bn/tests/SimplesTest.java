package bn.tests;

import bn.BayesNet;
import bn.DiscreteBNNode;
import bn.BayesNet.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;

public class SimplesTest {

	public static void main(String[] args) {
		try
		{
			double[][] cpt_values = {{.9, .1},{.25, .75}};
			BayesNet bn = new BayesNet();
			DiscreteBNNode root = bn.addDiscreteNode("root", 2);
			root.setDistribution(new DiscreteCPTUC(new double[]{.7, .3}));
			DiscreteBNNode child = bn.addDiscreteNode("child", 2);
			DiscreteCPT dist = new DiscreteCPT(new int[]{2}, 2,cpt_values);
			child.setDistribution(dist);
			child.addParent(root);
			root.addChild(child);
			child.setValue(1);
			bn.validate();
			bn.run(10, 1e-10);
			
			System.out.println("Root node probability : " + root.getMarginal().getValue(0) + "," + root.getMarginal().getValue(1));
			System.out.println("Child node probability : " + child.getMarginal().getValue(0) + "," + child.getMarginal().getValue(1));
			
		}
		catch(BNException e)
		{
			System.out.println("Error occurred while executing test : " + e.toString());
		}
	}

}

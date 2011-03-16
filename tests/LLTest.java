package tests;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.TrueOr;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

public class LLTest {
	public static void main(String[] args) throws BNException
	{
		IDynamicBayesNet network = DynamicNetworkFactory.newDynamicBayesNet(2);
		
		IFDiscDBNNode x1 = network.addDiscreteNode("X1", 2);
		IFDiscDBNNode x2 = network.addDiscreteNode("X2", 2);
		IFDiscDBNNode x3 = network.addDiscreteNode("X3", 2);
		
		DiscreteCPT A = new DiscreteCPT(new double[][]{{.9, .1}, {.1, .9}}, 2);
		DiscreteCPTUC pi = new DiscreteCPTUC(new double[]{.9, .1});
		x1.setInitialDistribution(pi);
		x1.setAdvanceDistribution(A);
		x2.setInitialDistribution(pi);
		x2.setAdvanceDistribution(A);
		x3.setInitialDistribution(pi);
		x3.setAdvanceDistribution(A);
		
		IFDiscDBNNode yor = network.addDiscreteNode("YOR", 2);
		yor.setAdvanceDistribution(TrueOr.getInstance());
		IFDiscDBNNode y = network.addDiscreteNode("Y", 2);
		y.setInitialDistribution(A);
		DiscreteCPT B = new DiscreteCPT(new int[]{2,2},2,new double[][]{{.97, .03},{.9, .1},{.5, .5},{.3, .7}});
		y.setAdvanceDistribution(B);
		
		y.setValue(new int[]{0,1},0);
		
		network.addInterEdge("Y", "Y");
		network.addInterEdge("X1", "X1");
		network.addInterEdge("X2", "X2");
		network.addInterEdge("X3", "X3");
		
		network.addIntraEdge("X1", "YOR");
		network.addIntraEdge("X3", "YOR");
		network.addIntraEdge("X2", "YOR");
		network.addIntraEdge("YOR","Y");
		
		network.validate();
		network.run();
		System.out.println(network.getLogLikelihood());
	}

}

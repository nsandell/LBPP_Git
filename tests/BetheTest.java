package tests;

import bn.BNException;
import bn.IDynBayesNet;
import bn.IStaticBayesNet;
import bn.IBayesNet.RunResults;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.ScalarNoisyOr;
import bn.impl.BayesNetworkFactory;
import bn.messages.DiscreteMessage;

public class BetheTest {

	public static void main(String[] args) throws BNException
	{
		IStaticBayesNet bn = BayesNetworkFactory.getStaticNetwork();
		bn.addDiscreteNode("C", 2);
		bn.addDiscreteNode("S", 2);
		bn.addDiscreteNode("R", 2);
		bn.addDiscreteNode("W", 2);
		
		bn.addEdge("C", "R");
		bn.addEdge("C", "S");
		bn.addEdge("S","W");
		bn.addEdge("R","W");
		
		
		bn.setDistribution("C", new DiscreteCPTUC(new double[]{.5, .5}));
		bn.setDistribution("S", new DiscreteCPT(new double[][]{{.5, .5},{.9, .1}},2));
		bn.setDistribution("R", new DiscreteCPT(new double[][]{{.8, .2},{.2, .8}},2));
		//bn.setDistribution("W", new DiscreteCPT(new int[]{2,2},2,new double[][]{{1, 0},{.1, .9},{.1, .9},{.01, .99}}));
		bn.addEvidence("W", 1);
		bn.setDistribution("W", new ScalarNoisyOr(.9));
		
		bn.validate();
		RunResults rr = bn.run(100, 0);
		System.out.println(rr.numIts + " : " + rr.error);
		System.out.println(((DiscreteMessage)bn.getMarginal("C")).getValue(0));
		System.out.println(((DiscreteMessage)bn.getMarginal("S")).getValue(0));
		System.out.println(((DiscreteMessage)bn.getMarginal("R")).getValue(0));
		System.out.println(((DiscreteMessage)bn.getMarginal("W")).getValue(0));
		System.out.println("BE : "+bn.getBetheEnergy());
		
		/*IDynBayesNet dbn = BayesNetworkFactory.getDynamicNetwork(2);
		dbn.addDiscreteNode("X", 2);
		dbn.addDiscreteNode("Y", 2);
		dbn.addIntraEdge("X", "Y");
		dbn.addInterEdge("X", "X");
		dbn.addInterEdge("Y", "Y");
		dbn.setInitialDistribution("X", new DiscreteCPTUC(new double[]{.5, .5}));
		dbn.setInitialDistribution("Y", new DiscreteCPT(new double[][]{{.5, .5},{.9, .1}},2));
		dbn.setDistribution("X", new DiscreteCPT(new double[][]{{.8, .2},{.2, .8}},2));
		dbn.setDistribution("Y", new DiscreteCPT(new int[]{2,2},2,new double[][]{{1, 0},{.1, .9},{.1, .9},{.01, .99}}));
		dbn.setEvidence("Y", 1, 1);
		dbn.validate();
		rr = dbn.run(100,0);
		System.out.println("DBN Impl Result: " + rr.numIts + " iterations");
		System.out.println(((DiscreteMessage)dbn.getMarginal("X",0)).getValue(0));
		System.out.println(((DiscreteMessage)dbn.getMarginal("X",1)).getValue(0));
		System.out.println(((DiscreteMessage)dbn.getMarginal("Y",0)).getValue(0));
		System.out.println(((DiscreteMessage)dbn.getMarginal("Y",1)).getValue(0));
		System.out.println("LL:"+dbn.getLogLikelihood());*/
	}
}

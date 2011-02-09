package tests;

import bn.BNException;
import bn.IBayesNet.RunResults;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.ScalarNoisyOr;
import bn.impl.staticbn.StaticNetworkFactory;
import bn.statc.IDiscreteBayesNode;
import bn.statc.IStaticBayesNet;

public class BetheTest2 {

	public static void main(String[] args) throws BNException
	{
		IStaticBayesNet bn = StaticNetworkFactory.getNetwork();
		IDiscreteBayesNode s = bn.addDiscreteNode("S", 2);
		IDiscreteBayesNode r = bn.addDiscreteNode("R", 2);
		IDiscreteBayesNode r2 = bn.addDiscreteNode("R2", 2);
		IDiscreteBayesNode r3 = bn.addDiscreteNode("R3", 2);
		IDiscreteBayesNode w = bn.addDiscreteNode("W", 2);
		
		bn.addEdge("S","W");
		bn.addEdge("R","W");
		bn.addEdge("R2","W");
		bn.addEdge("R3","W");
		
		
		bn.setDistribution("S", new DiscreteCPTUC(new double[]{.6, .4}));
		bn.setDistribution("R", new DiscreteCPTUC(new double[]{.8, .2}));
		bn.setDistribution("R2", new DiscreteCPTUC(new double[]{.9, .1}));
		bn.setDistribution("R3", new DiscreteCPTUC(new double[]{.7, .3}));
		//bn.setDistribution("W", new DiscreteCPT(new int[]{2,2},2,new double[][]{{1, 0},{.1, .9},{.1, .9},{.01, .99}}));
		w.setValue(0);
		s.setValue(0);
		r.setValue(0);
		bn.setDistribution("W", new ScalarNoisyOr(.9));
		
		bn.validate();
		RunResults rr = bn.run(100, 0);
		System.out.println(rr.numIts + " : " + rr.error);
		System.out.println(s.getMarginal().getValue(0));
		System.out.println(r.getMarginal().getValue(0));
		System.out.println(w.getMarginal().getValue(0));
		System.out.println("BE : " + bn.getLogLikelihood());
		
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

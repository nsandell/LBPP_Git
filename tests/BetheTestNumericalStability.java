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

public class BetheTestNumericalStability {

	public static void main(String[] args) throws BNException
	{
		for(int N = 3 ; N < 40; N++)
		{
			IStaticBayesNet bn = BayesNetworkFactory.getStaticNetwork();
			bn.addDiscreteNode("Child", 2);

			for(int i = 0; i < N; i++)
			{
				bn.addDiscreteNode("Parent"+i, 2);
				bn.addEdge("Parent"+i, "Child");
				bn.setDistribution("Parent"+i, new DiscreteCPTUC(new double[]{1-1e-4,1e-4}));
			}
			bn.setDistribution("Parent0", new DiscreteCPTUC(new double[]{1e-4,1-1e-4}));

			bn.addEvidence("Child", 0);
			bn.setDistribution("Child", new ScalarNoisyOr(.9));

			bn.validate();
			RunResults rr = bn.run(100, 0);
			System.out.println("N : " + N);
			System.out.println(rr.numIts + " : " + rr.error);
			System.out.println("BE : " + bn.getLogLikelihood());
			bn.addEvidence("Child", 1);
			rr = bn.run(100, 0);
			System.out.println(rr.numIts + " : " + rr.error);
			System.out.println("BE : " + bn.getLogLikelihood());
		}
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

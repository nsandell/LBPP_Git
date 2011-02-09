package tests;

import bn.BNException;

import bn.IBayesNet.RunResults;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.ScalarNoisyOr;
import bn.impl.staticbn.StaticNetworkFactory;
import bn.statc.IDiscreteBayesNode;
import bn.statc.IStaticBayesNet;

public class BetheTestNumericalStability {

	public static void main(String[] args) throws BNException
	{
		for(int N = 6; N < 40; N++)
		{
			IStaticBayesNet bn = StaticNetworkFactory.getNetwork();
			IDiscreteBayesNode child = bn.addDiscreteNode("Child", 2);
			
			double eps = 1e-7;

			for(int i = 0; i < N; i++)
			{
				bn.addDiscreteNode("Parent"+i, 2);
				bn.addEdge("Parent"+i, "Child");
				bn.setDistribution("Parent"+i, new DiscreteCPTUC(new double[]{1-eps,eps}));
			}
			bn.setDistribution("Parent0", new DiscreteCPTUC(new double[]{eps,1-eps}));
			bn.setDistribution("Parent1", new DiscreteCPTUC(new double[]{eps,1-eps}));
			bn.setDistribution("Parent2", new DiscreteCPTUC(new double[]{eps,1-eps}));
			bn.setDistribution("Parent3", new DiscreteCPTUC(new double[]{eps,1-eps}));
			bn.setDistribution("Parent4", new DiscreteCPTUC(new double[]{eps,1-eps}));
			bn.setDistribution("Parent5", new DiscreteCPTUC(new double[]{eps,1-eps}));

			child.setValue(0);
			bn.setDistribution("Child", new ScalarNoisyOr(.9));

			bn.validate();
			RunResults rr = bn.run(100, 0);
			System.out.println("N : " + N);
			System.out.println(rr.numIts + " : " + rr.error);
			System.out.println("BE : " + bn.getLogLikelihood());
			child.setValue(1);
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

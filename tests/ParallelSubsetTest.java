package tests;

import java.util.Vector;

import bn.BNException;
import bn.IBayesNode;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

public class ParallelSubsetTest {
	public static void main(String[] args) throws Exception
	{
		int length = 3000;
		try
		{
			System.out.println("Seq length = " + length);

			double[][] A = {{.85, .05, .05, .05},{ .05, .85, .05, .05},{.05, .05,  .85, .05},{.05, .05, .05, .85}};
			double[][] B = {{.9, .1},{.1, .9},{.9, .1},{.1, .9}};
			double[] pi = {.25,.25,.25,.25};
			DiscreteCPT ACPT = new DiscreteCPT(new int[]{4}, 4, A);
			DiscreteCPT BCPT = new DiscreteCPT(new int[]{4}, 2, B);
			DiscreteCPTUC piCPT = new DiscreteCPTUC(pi);

			int[] obs = new int[length];
			for(int i = 0; i < length; i++)
			{
				obs[i] = i % 2;
			}
			
			int[] obs2 = new int[length];
			for(int i = 0; i < length; i++)
			{
				obs2[i] = (i+1) % 2;
			}

			IDynamicBayesNet dbn = DynamicNetworkFactory.newDynamicBayesNet(length); 
			IFDiscDBNNode y = dbn.addDiscreteNode("y", 2);
			IFDiscDBNNode y2 = dbn.addDiscreteNode("y2", 2);
			IFDiscDBNNode x = dbn.addDiscreteNode("x", 4);
			IBayesNode x2 = dbn.addDiscreteNode("x2", 4);
			dbn.addIntraEdge(x, y);
			dbn.addIntraEdge(x2, y2);
			dbn.addInterEdge(x, x);
			dbn.addInterEdge(x2, x2);
			dbn.setInitialDistribution(x.getName(), piCPT);
			dbn.setInitialDistribution(x2.getName(), piCPT);
			dbn.setAdvanceDistribution(x.getName(), ACPT);
			dbn.setAdvanceDistribution(x2.getName(), ACPT);
			dbn.setAdvanceDistribution(y.getName(), BCPT);
			dbn.setAdvanceDistribution(y2.getName(), BCPT);
			y.setValue(obs, 0);
			y2.setValue(obs, 0);

			dbn.validate();

			Vector<String> seeds = new Vector<String>();
			seeds.add("x");
			seeds.add("x2");
			
			dbn.run_subsets_parallel(seeds, 10, 0);
			y2.setValue(obs2, 0);
			dbn.run_subsets_parallel(seeds, 10, 0);
			y.setValue(obs2, 0);
			dbn.run_subsets_parallel(seeds, 10, 0);
		}
		catch(BNException e) {
			System.err.println("Error while running HMMtest : " + e.toString());
		}
	}
}

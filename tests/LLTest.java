package tests;


import bn.BNException;


import bn.IBayesNode;
import bn.IDynBayesNet;
import bn.IBayesNet.RunResults;
import bn.IDynBayesNet.ParallelCallback;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.ScalarNoisyOr;
import bn.impl.BayesNetworkFactory;
import bn.messages.DiscreteMessage;

public class LLTest {
	public static void main(String[] args)
	{
		int length = 2500;
		try
		{
			double[][] A = {{.85, .15},{ .15, .85}};
			double[][] B = {{1,0},{.1,.9},{.1,.9},{.01,.99}};
			double[] pi = {.8,.2};
			DiscreteCPT ACPT = new DiscreteCPT(new int[]{2}, 2, A);
			DiscreteCPT BCPT = new DiscreteCPT(new int[]{2,2}, 2, B);
			DiscreteCPTUC piCPT = new DiscreteCPTUC(pi);

			Integer[] obs = new Integer[length];
			for(int i = 0; i < length; i++)
			{
				obs[i] = i % 2;
			}
			
			IDynBayesNet dbn = BayesNetworkFactory.getDynamicNetwork(length);
			IBayesNode y = dbn.addDiscreteNode("y", 2);
			IBayesNode x = dbn.addDiscreteNode("x", 2);
			IBayesNode x2 = dbn.addDiscreteNode("x2", 2);
			dbn.addIntraEdge(x, y);
			dbn.addIntraEdge(x2, y);
			dbn.addInterEdge(x, x);
			dbn.addInterEdge(x2, x2);
			dbn.setInitialDistribution(x.getName(), piCPT);
			dbn.setInitialDistribution(x2.getName(), piCPT);
			dbn.setDistribution(x.getName(), ACPT);
			dbn.setDistribution(x2.getName(), ACPT);
			//dbn.setDistribution(y.getName(), new ScalarNoisyOr(.9));
			dbn.setDistribution(y.getName(), BCPT);
			dbn.setEvidence(y.getName(), 0, obs);

			dbn.validate();

			System.out.println("Running serially");
			long begin = System.currentTimeMillis();
			System.out.println("Num Iterations " + dbn.run(1000,1e-4).numIts + " out of 100");
			dbn.run(1,0);
			long end = System.currentTimeMillis();
			double runtime = ((double)(end-begin))/1000;
			for(int i = 0; i < Math.min(length, 10); ++i)
			{
				DiscreteMessage msg = (DiscreteMessage)dbn.getMarginal("x", i);
				System.out.println("X("+i+"): ["+msg.getValue(0) +"," + msg.getValue(1)+"]");
			}
			System.out.println("Converged in " + runtime + " seconds... X Probabilities");
			System.out.println("BFE : " + dbn.getLogLikelihood());
		}
		catch(BNException e) {
			System.err.println("Error while running HMMtest : " + e.toString());
		}
	}


	public static class CallbackClass implements ParallelCallback
	{

		public CallbackClass(long starttime)
		{
			this.starttime = starttime;
		}

		public void callback(IDynBayesNet dbn,int numIts, double time, double error)
		{
			try
			{
				long end = System.currentTimeMillis();
				for(int i = 0; i < 10; i+=1)
				{
					DiscreteMessage marginal = (DiscreteMessage)dbn.getMarginal("x", i);
					System.out.println("X: ["+marginal.getValue(0) +"," + marginal.getValue(1)+"]");
				}
				double runtime = ((double)(end-starttime))/1000;
				System.out.println("Parallel inference converged in " + runtime + " seconds... ");
			}
			catch(BNException e) {
				System.err.println("Eh? : " + e.toString());
			}
		}

		public void error(IDynBayesNet dbn, String error)
		{
			System.out.println("Error: " + error);
		}
		long starttime;
	}

}

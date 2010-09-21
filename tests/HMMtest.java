package tests;


import java.util.Random;

import bn.BNException;
import bn.BayesNetworkFactory;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.interfaces.IDiscreteDynBayesNode;
import bn.interfaces.IDynBayesNet;
import bn.interfaces.IDynBayesNet.ParallelInferenceCallback;
import bn.interfaces.IDynBayesNode;

public class HMMtest {
	public static void main(String[] args)
	{
		int length = Integer.parseInt(args[0]);
		try
		{
			System.out.println("Seq length = " + length);
			double[][] A = {{.7, .3},{.3, .7}};
			double[][] B = {{.9, .1},{.35, .65}};
			double[] pi = {1,0};
			DiscreteCPT ACPT = new DiscreteCPT(new int[]{2}, 2, A);
			DiscreteCPT BCPT = new DiscreteCPT(new int[]{2}, 2, B);
			DiscreteCPTUC piCPT = new DiscreteCPTUC(pi);
			
			//int[] obs = {0,1,1,1,0,0,0,0,0,0};
			int[] obs = new int[length];
			//Random rand = new Random();
			for(int i = 0; i < length; i++)
			{
				obs[i] = i % 2;
				/*if(rand.nextBoolean())
					obs[i] = 1;
				else
					obs[i] = 0;*/
			}
			//int[] obs = {0,1};
			IDynBayesNet dbn = BayesNetworkFactory.getDynamicNetwork(length);
		
			IDiscreteDynBayesNode y = dbn.addDiscreteNode("y", 2);
			IDiscreteDynBayesNode x = dbn.addDiscreteNode("x", 2);
			dbn.addIntraEdge(x, y);
			dbn.addInterEdge(x, x);
			x.setInitialDistribution(piCPT);
			x.setAdvanceDistribution(ACPT);
			y.setAdvanceDistribution(BCPT);
			y.setInitialDistribution(BCPT);
			y.setValue(obs, 0);
	
//			long begin = System.currentTimeMillis();
//			dbn.run(100, 1e-6);
//			long end = System.currentTimeMillis();
//			double runtime = ((double)(end-begin))/1000;
//			System.out.println("Converged in " + runtime + " seconds... X Probabilities");
			
//			for(int i = 0; i < 10000; i+=100)
//			{
//				IDiscreteBayesNode inst = x.getDiscreteInstance(i);
//				System.out.println(inst.getName()+": ["+x.getMarginal(i).getValue(0) +"," + x.getMarginal(i).getValue(1)+"]");
//			}
			dbn.run_parallel(100, 1e-6, new CallbackClass(System.currentTimeMillis()));
		}
		catch(BNException e) {
			System.err.println("Error while running HMMtest : " + e.toString());
		}
	}

	public static class CallbackClass implements ParallelInferenceCallback
	{
		public CallbackClass(long starttime)
		{
			this.starttime = starttime;
		}
		
		public void callback(IDynBayesNet dbn)
		{
			long end = System.currentTimeMillis();
			double runtime = ((double)(end-starttime))/1000;
			System.out.println("Parallel inference converged in " + runtime + " seconds... ");
		}
		
		public void error(IDynBayesNet dbn, String error)
		{
			System.out.println("Error: " + error);
		}
		long starttime;
	}
	
}

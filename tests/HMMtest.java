package tests;


import bn.BNException;
import bn.BayesNetworkFactory;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.interfaces.IDiscreteBayesNode;
import bn.interfaces.IDiscreteDynBayesNode;
import bn.interfaces.IDynBayesNet;
import bn.interfaces.IDynBayesNet.ParallelInferenceCallback;

public class HMMtest {
	public static void main(String[] args)
	{
		int length = Integer.parseInt(args[0]);
		String parallel = args[1];
		try
		{
			System.out.println("Seq length = " + length);
			double[][] A = {{.7, .3},{.3, .7}};
			double[][] B = {{.9, .1},{.35, .65}};
			double[] pi = {1,0};
			DiscreteCPT ACPT = new DiscreteCPT(new int[]{2}, 2, A);
			DiscreteCPT BCPT = new DiscreteCPT(new int[]{2}, 2, B);
			DiscreteCPTUC piCPT = new DiscreteCPTUC(pi);

			int[] obs = new int[length];
			for(int i = 0; i < length; i++)
			{
				obs[i] = i % 2;
			}
			IDynBayesNet dbn = BayesNetworkFactory.getDynamicNetwork(length);
			IDiscreteDynBayesNode y = dbn.addDiscreteNode("y", 2);
			IDiscreteDynBayesNode x = dbn.addDiscreteNode("x", 2);
			dbn.addIntraEdge(x, y);
			dbn.addInterEdge(x, x);
			x.setInitialDistribution(piCPT);
			x.setAdvanceDistribution(ACPT);
			y.setInitialDistribution(BCPT);
			y.setAdvanceDistribution(BCPT);
			y.setValue(obs, 0);

			dbn.validate();
			
			if(parallel.compareTo("serial")==0)
			{

				long begin = System.currentTimeMillis();
				dbn.run(100, 0.0);
				long end = System.currentTimeMillis();
				double runtime = ((double)(end-begin))/1000;
				for(int i = 0; i < 2; ++i)
				{
					IDiscreteBayesNode inst = x.getDiscreteInstance(i);
					System.out.println(inst.getName()+": ["+x.getMarginal(i).getValue(0) +"," + x.getMarginal(i).getValue(1)+"]");
				}
				System.out.println("Converged in " + runtime + " seconds... X Probabilities");
				System.out.println("Observation likelihood : " + y.getLogLikelihood());
			}
			else
				dbn.run_parallel(100, 0, new CallbackClass(System.currentTimeMillis(),x,y));
		}
		catch(BNException e) {
			System.err.println("Error while running HMMtest : " + e.toString());
		}
	}


	public static class CallbackClass implements ParallelInferenceCallback
	{

		private  IDiscreteDynBayesNode x, y;
		public CallbackClass(long starttime, IDiscreteDynBayesNode x, IDiscreteDynBayesNode y)
		{
			this.x = x;
			this.y = y;
			this.starttime = starttime;
		}

		public void callback(IDynBayesNet dbn)
		{
			try
			{
				long end = System.currentTimeMillis();
				for(int i = 0; i < 10; i+=1)
				{
					IDiscreteBayesNode inst = x.getDiscreteInstance(i);
					System.out.println(inst.getName()+": ["+x.getMarginal(i).getValue(0) +"," + x.getMarginal(i).getValue(1)+"]");
				}
				System.out.println("Observation likelihood : " + y.getLogLikelihood());
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

package tests;


import java.io.PrintStream;

import bn.BNException;


import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IDynamicBayesNet.ParallelCallback;
import bn.impl.dynbn.DynamicNetworkFactory;
import bn.messages.FiniteDiscreteMessage;

public class HMMtest {
	public static void main(String[] args) throws Exception
	{
		int[][] data = CrawdadMFHMMLearnText.loadData(args[0]);
		for(int i = 0; i < data.length; i++)
			for(int j = 0; j < data[i].length; j++)
				data[i][j]--;
		try
		{
			double[][] A = {{.9, .05, .05},{.05, .9, .05},{.05, .05, .9}};
			double[][] B = {{.9,.05,.05},{.05,.9,.05},{.05,.05,.9}};
			double[] pi = {1,0,0};
			DiscreteCPT ACPT = new DiscreteCPT(new int[]{3}, 3, A);
			DiscreteCPTUC piCPT = new DiscreteCPTUC(pi);
			
			int Nobs = data.length;
			int T = data[0].length;
			
			IDynamicBayesNet dbn = DynamicNetworkFactory.newDynamicBayesNet(T);
			IFDiscDBNNode x = dbn.addDiscreteNode("x", 3);
			dbn.addInterEdge(x, x);
			dbn.setInitialDistribution(x.getName(), piCPT);
			dbn.setAdvanceDistribution(x.getName(), ACPT);

			for(int i = 0; i < Nobs; i++)
			{
				IFDiscDBNNode y = dbn.addDiscreteNode("Y"+i, 3);
				y.setAdvanceDistribution(new DiscreteCPT(new int[]{3},3,B));
				y.setValue(data[i], 0);
				dbn.addIntraEdge(x, y);
			}

			dbn.validate();
			dbn.optimize_parallel(40, 1e-6, 20, 0);
			dbn.print(new PrintStream(args[1]));
			System.out.println(dbn.getLogLikelihood());
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

		public void callback(IDynamicBayesNet dbn,int numIts, double time, double error)
		{
			try
			{
				long end = System.currentTimeMillis();
				for(int i = 0; i < 10; i+=1)
				{
					FiniteDiscreteMessage marginal = ((IFDiscDBNNode)dbn.getNode("x")).getMarginal(i);
					System.out.println("X: ["+marginal.getValue(0) +"," + marginal.getValue(1)+"]");
				}
				double runtime = ((double)(end-starttime))/1000;
				System.out.println("Parallel inference converged in " + runtime + " seconds... ");
			}
			catch(BNException e) {
				System.err.println("Eh? : " + e.toString());
			}
		}

		public void error(IDynamicBayesNet dbn, String error)
		{
			System.out.println("Error: " + error);
		}
		long starttime;
	}

}

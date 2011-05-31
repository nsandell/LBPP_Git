package tests;

import java.io.PrintStream;

import util.MathUtil;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IDynamicBayesNet.ParallelCallback;
import bn.impl.dynbn.DynamicNetworkFactory;
import bn.messages.FiniteDiscreteMessage;

public class ThesisFHMMOpt {
	public static void main(String[] args) throws Exception
	{
		int[][] data = CrawdadMFHMMLearnText.loadData(args[0]);
		/*for(int i = 0; i < data.length; i++)
			for(int j = 0; j < data[i].length; j++)
				data[i][j]--;*/
		
		try
		{
			double[][] A = {{.95, .05}, {.05, .95}};
			double[] pi = {1,0};
			DiscreteCPT ACPT = new DiscreteCPT(new int[]{2}, 2, A);
			DiscreteCPTUC piCPT = new DiscreteCPTUC(pi);
			
			int Nobs = data.length;
			int T = data[0].length;

			IDynamicBayesNet dbn = DynamicNetworkFactory.newDynamicBayesNet(T);

			for(int i = 0; i < 4; i++)
			{
				IFDiscDBNNode x = dbn.addDiscreteNode("x"+i, 2);
				dbn.addInterEdge(x, x);
				dbn.setInitialDistribution(x.getName(), piCPT);
				dbn.setAdvanceDistribution(x.getName(), ACPT);
			}

			for(int i = 0; i < Nobs; i++)
			{
				IFDiscDBNNode y = dbn.addDiscreteNode("Y"+i, 3);
				int nump = 4;
				double[][] B = getB(nump,3);
				int[] pds = new int[nump];
				for(int j = 0; j < nump; j++)
					pds[j] = 2;
				y.setAdvanceDistribution(new DiscreteCPT(pds,3,B));
				y.setValue(data[i], 0);
			}
			
			for(int i = 0; i < 10; i++)
				for(int j = 0; j < 4; j++)
					dbn.addIntraEdge("x"+j, "Y"+i);

			try {
				dbn.validate();
			} catch(Exception e) {
				dbn.print(new PrintStream("dump_model"));
			}
			dbn.optimize_parallel(40, 1e-6, 40, 1e-6);
			dbn.print(new PrintStream(args[1]));
			System.out.println(dbn.getLogLikelihood());
		}
		catch(BNException e) {
			System.err.println("Error while running HMMtest : " + e.toString());
		}
	}
	
	public static double[][] getB(int np, int no)
	{
		int ss = (int)Math.pow(2,np);
		double[][] B = new double[ss][no];
	
		if(no > ss)
		{
			for(int i = 0; i < ss; i++)
			{
				int prim = i % ss;
				double sum = 0;
				for(int j = 0; j < no; j++)
				{
					if(j==prim)
						continue;
					B[i][j] = .1*MathUtil.rand.nextDouble();
					sum += B[i][j];
				}
				B[i][prim] = 1-sum;
			}
		}
		else
		{
			for(int i = 0; i < ss; i++)
			{
				int prim = MathUtil.rand.nextInt(no);
				double sum = 0;
				for(int j = 0; j < no; j++)
				{
					if(j==prim)
						continue;
					B[i][j] = .1*MathUtil.rand.nextDouble();
					sum += B[i][j];
				}
				B[i][prim] = 1-sum;
			}
		}
		
		return B;
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

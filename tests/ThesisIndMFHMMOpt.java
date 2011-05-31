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

public class ThesisIndMFHMMOpt {
	public static void main(String[] args) throws Exception
	{
		int[][] data = CrawdadMFHMMLearnText.loadData(args[0]);
		/*for(int i = 0; i < data.length; i++)
			for(int j = 0; j < data[i].length; j++)
				data[i][j]--;*/
		
		int[][] F = CrawdadMFHMMLearnText.loadData(args[1]);
		
		try
		{
			double[][] A = {{.95, .05}, {.5, .5}};
			double[] pi = {1,0};
			DiscreteCPT ACPT = new DiscreteCPT(new int[]{2}, 2, A);
			DiscreteCPTUC piCPT = new DiscreteCPTUC(pi);
			
			int Nobs = data.length;
			int T = data[0].length;



			for(int i = 0; i < Nobs; i++)
			{
				IDynamicBayesNet dbn = DynamicNetworkFactory.newDynamicBayesNet(T);
				IFDiscDBNNode y = dbn.addDiscreteNode("Y", 3);

				for(int j = 0; j < 4; j++)
				{
					if(F[i][j]==1)
					{
						IFDiscDBNNode x = dbn.addDiscreteNode("X"+j, 2);
						dbn.addInterEdge(x, x);
						dbn.addIntraEdge(x, y);
						dbn.setInitialDistribution(x.getName(), piCPT);
						dbn.setAdvanceDistribution(x.getName(), ACPT);
					}
				}

				int nump = 0;
				for(int j = 0; j < F[i].length; j++)
					nump += F[i][j];
				double[][] B = getB(nump,3);
				int[] pds = new int[nump];
				for(int j = 0; j < nump; j++)
					pds[j] = 2;
				y.setAdvanceDistribution(new DiscreteCPT(pds,3,B));
				y.setValue(data[i], 0);

				try {
					dbn.validate();
				} catch(Exception e) {
					dbn.print(new PrintStream("dump_model"));
				}
				dbn.optimize_parallel(80, 1e-6, 80, 1e-6);
				dbn.print(new PrintStream(args[2]+i));
				System.out.println(dbn.getLogLikelihood());
			}
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
				for(int j = 0; j < no; j++)
				{
					B[i][j] = .03;
				}
				B[i][prim] = 1-(no-1)*.03;
			}
		}
		else
		{
			for(int i = 0; i < ss; i++)
			{
				int prim = MathUtil.rand.nextInt(no);
				for(int j = 0; j < no; j++)
				{
					B[i][j] = .03;
				}
				B[i][prim] = 1-(no-1)*.03;
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

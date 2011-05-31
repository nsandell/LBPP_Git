package tests;


import bn.BNException;


import bn.commandline.DynamicCommandLine;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IDynamicBayesNet.ParallelCallback;
import bn.messages.FiniteDiscreteMessage;

public class HMMlleval {
	public static void main(String[] args) throws Exception
	{
		int[][] data = CrawdadMFHMMLearnText.loadData(args[0]);
		/*for(int i = 0; i < data.length; i++)
			for(int j = 0; j < data[i].length; j++)
				data[i][j]--;*/
		try
		{
			IDynamicBayesNet dbn = DynamicCommandLine.loadNetwork(args[1]);

			for(int i = 0; i < data.length; i++)
			{
				IFDiscDBNNode y = (IFDiscDBNNode)dbn.getNode("Y"+i);
				y.setValue(data[i], 0);
			}

			dbn.validate();
			dbn.run_parallel_block(40, 0.0);
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

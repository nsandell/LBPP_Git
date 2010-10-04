package tests;


import bn.BNException;

import bn.Options.InferenceOptions;

import bn.IDynBayesNet;
import bn.Options;
import bn.commandline.DynamicCommandLine;

public class HMMLearntest {
	public static void main(String[] args)
	{
		String parallel = args[0];
		try
		{
			IDynBayesNet dbn = DynamicCommandLine.loadNetwork("/Users/nsandell/LBPPack/trunk/test_files/hmmLearn.dlbp");
			dbn.collectSufficientStats(true);
			dbn.validate();
			
			for(int it = 0; it < 500; it++)
			{
				System.out.println("Learning iteration " + it);
				if(parallel.compareTo("serial")==0)
				{
					long begin = System.currentTimeMillis();
					dbn.run(new InferenceOptions(10, 0.0));
					long end = System.currentTimeMillis();
					double runtime = ((double)(end-begin))/1000;
					System.out.println("Converged in " + runtime + " seconds...");
				}
				else
				{
					InferenceOptions opts = new InferenceOptions(10,0);
					opts.parallel = true;
					dbn.run(opts);
				}
				dbn.optimize(new Options.LearningOptions());
			}
			int c = 3;
			c*=3;
		}
		catch(BNException e) {
			System.err.println("Error while running HMMtest : " + e.toString());
		}
	}
}

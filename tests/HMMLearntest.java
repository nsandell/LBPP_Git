package tests;


import bn.BNException;
import bn.IDiscreteDynBayesNode;

import bn.IDynBayesNet;
import bn.commandline.DynamicCommandLine;

public class HMMLearntest {
	public static void main(String[] args)
	{
		String parallel = args[0];
		try
		{
			IDynBayesNet dbn = DynamicCommandLine.loadNetwork("/Users/nsandell/LBPPack/trunk/test_files/hmmLearn.dlbp");
			dbn.validate();
			
			for(int it = 0; it < 28; it++)
			{
				System.out.println("Learning iteration " + it);
				if(parallel.compareTo("serial")==0)
				{
					long begin = System.currentTimeMillis();
					dbn.run(100,0);
					long end = System.currentTimeMillis();
					double runtime = ((double)(end-begin))/1000;
					System.out.println("Converged in " + runtime + " seconds...");
				}
				else
					dbn.run_parallel_block(100, 0);
				for(int j = 0; j < 2; j++)
				{
					for(int i = 0; i < 3; i++)
					{
						System.out.print(((IDiscreteDynBayesNode)dbn.getNode("X")).getMarginal(i).getValue(j)+ " ");
					}System.out.println();
				}
				dbn.optimize();
			}
		}
		catch(BNException e) {
			System.err.println("Error while running HMMtest : " + e.toString());
		}
	}
}
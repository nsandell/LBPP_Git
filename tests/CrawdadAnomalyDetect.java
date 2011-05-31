package tests;

import java.io.PrintStream;
import java.util.Vector;

import bn.IBayesNet.RunResults;
import bn.commandline.DynamicCommandLine;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

public class CrawdadAnomalyDetect {

	public static void main(String[] args) throws Exception
	{
		String obs_file = args[0];
		String ass_file = args[1];//model file in detect mode
		String mode = args[2];
		
		int[][] o = CrawdadMFHMMLearnText.loadData(obs_file);
	
		/***
		 * Perform permutations of each playa here
		 */

		if(mode.equals("build"))
		{
			IDynamicBayesNet network = DynamicNetworkFactory.newDynamicBayesNet(o[0].length);

			int[][] assignments =  CrawdadMFHMMLearnText.loadData(ass_file);
			int numAssignments = 0;
			for(int j = 0; j < assignments[0].length; j++)
				numAssignments = Math.max(numAssignments, assignments[0][j]+1);

			Vector<IFDiscDBNNode> parents = new Vector<IFDiscDBNNode>();
			for(int j = 0; j < numAssignments; j++)
			{
				IFDiscDBNNode nd = network.addDiscreteNode("X"+j, 2);
				nd.setInitialDistribution(new DiscreteCPTUC(new double[]{.9,.1}));
				nd.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}},2));
				network.addInterEdge(nd, nd);
				parents.add(nd);
			}

			Vector<IFDiscDBNNode> children = new Vector<IFDiscDBNNode>();
			for(int i = 0; i < o.length; i++)
			{
				IFDiscDBNNode nd = network.addDiscreteNode("Y"+i, 2);
				children.add(nd);
				network.addInterEdge(nd, nd);
				network.addIntraEdge(parents.get(assignments[0][i]),nd);
				nd.setAdvanceDistribution(new DiscreteCPT(new int[]{2,2},2,new double[][]{{1-1e-9,1e-9},{1-1e-7,1e-7},{.5,.5},{.3,.7}}));
				nd.setInitialDistribution(new DiscreteCPT(new double[][]{{1-1e-9,1e-9},{.5,.5}},2));
				nd.setValue(o[i], 0);
			}

			network.optimize_parallel(20,1e-5,20,1e-5);
			network.run_parallel_block(100, 0);

			PrintStream ps = new PrintStream("./model.lbp");
			network.print(ps);

			for(int i = 0; i < children.size(); i++)
			{
				IFDiscDBNNode child = children.get(i);
				for(int t = 0; t < network.getT(); t++)
					System.out.print(child.conditionalLL(t) + " ");
				System.out.println();
			}
		}
		else if(mode.equals("build2"))
		{
			IDynamicBayesNet network = DynamicCommandLine.loadNetwork(ass_file);
			
			network.run_parallel_queue(100, 0);
			
			for(int i = 0; i < o.length; i++)
			{
				IFDiscDBNNode nd = (IFDiscDBNNode) network.getNode("Y"+i);
				for(int t = 0; t < network.getT(); t++)
					System.out.print(nd.conditionalLL(t) + " ");
				System.out.println();
			}
		}
		else if(mode.equals("new"))
		{
			IDynamicBayesNet network = DynamicCommandLine.loadNetwork(ass_file);
			
			RunResults res =  network.run_parallel_queue(20, 0);
			System.err.println("NUMITS:" + res.numIts);
			
			
			for(int i = 0; i < o.length; i++)
			{
				int[] obs = new int[4032];
				for(int t = 0; t < 4032; t++)
					obs[t] = o[i][t+1];
				IFDiscDBNNode nd = (IFDiscDBNNode) network.getNode("Y"+(o[i][0]-1));
				int[] backup = new int[4032];
				for(int t = 0; t < 4032; t++)
					backup[t] = nd.getValue(t);
				nd.setValue(obs, 0);
				System.out.print(o[i][0]);
				for(int t = 0; t < network.getT(); t++)
					System.out.print(" " + nd.conditionalLL(t));
				System.out.println();
				nd.setValue(backup, 0);
			}
		}
		else
		{
			IDynamicBayesNet network = DynamicCommandLine.loadNetwork(ass_file);
			
			for(int i = 0; i < o.length; i++)
			{
				IFDiscDBNNode nd = (IFDiscDBNNode) network.getNode("Y"+i);
				nd.setValue(o[i], 0);
			}
			network.run_parallel_queue(100, 0);
			

			for(int i = 0; i < o.length; i++)
			{
				IFDiscDBNNode nd = (IFDiscDBNNode) network.getNode("Y"+i);
				for(int t = 0; t < network.getT(); t++)
					System.out.print(nd.conditionalLL(t) + " ");
				System.out.println();
			}
		}
	}
}

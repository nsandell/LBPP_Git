package tests;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import bn.IDiscreteDynBayesNode;
import bn.IDynBayesNet;
import bn.IBayesNet.RunResults;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.impl.BayesNetworkFactory;
import bn.impl.CompositeDBN;
import bn.impl.IDBNFragmentServer;
import bn.messages.DiscreteMessage;

public class RemoteFragmentTest {
	public static void main(String[] args)
	{
		System.setSecurityManager(new RMISecurityManager());
		
		IDBNFragmentServer server = null;
		try
		{
			Registry reg = LocateRegistry.getRegistry("127.0.0.1", 1099);
			server = (IDBNFragmentServer)reg.lookup("DBNFragmentServer");
			IDBNFragmentServer[] servers = new IDBNFragmentServer[3];
			servers[0] = server;
			servers[1] = (IDBNFragmentServer)Naming.lookup("//ddesk:1199/DBNFragmentServer");
			servers[2] = (IDBNFragmentServer)Naming.lookup("//rocket.ists.dartmouth.edu:1200/DBNFragmentServer");
	
			double[][] A = {{.85, .05, .05, .05},{.05, .85, .05, .05},{.05, .05, .85, .05},{.05, .05, .05, .85}};
			double[][] B = {{.9, .1},{.1, .9},{.3, .7},{.7, .3}};
			double[] pi = {.8,.1, .05, .05};
			DiscreteCPT ACPT = new DiscreteCPT(new int[]{4}, 4, A);
			DiscreteCPT BCPT = new DiscreteCPT(new int[]{4}, 2, B);
			DiscreteCPTUC piCPT = new DiscreteCPTUC(pi);
			
			CompositeDBN cdbn = new CompositeDBN(8000, "testDBN", new double[] {.1,.45,.45}, servers);
			IDynBayesNet idbn = BayesNetworkFactory.getDynamicNetwork(8000);

			cdbn.addDiscreteNode("X", 4);
			cdbn.addDiscreteNode("Y", 2);
			cdbn.addDiscreteNode("Y2", 2);
			cdbn.addDiscreteNode("Y3", 2);
			cdbn.addDiscreteNode("Y4", 2);
			cdbn.addInterEdge("X", "X");
			cdbn.addIntraEdge("X", "Y");
			cdbn.addIntraEdge("X", "Y2");
			cdbn.addIntraEdge("X", "Y3");
			cdbn.addIntraEdge("X", "Y4");
			cdbn.setInitialDistribution("X", piCPT);
			cdbn.setDistribution("X", ACPT);
			cdbn.setDistribution("Y", BCPT);
			cdbn.setDistribution("Y2", BCPT);
			cdbn.setDistribution("Y3", BCPT);
			cdbn.setDistribution("Y4", BCPT);
			
			idbn.addDiscreteNode("X", 4);
			idbn.addDiscreteNode("Y", 2);
			idbn.addDiscreteNode("Y2", 2);
			idbn.addDiscreteNode("Y3", 2);
			idbn.addDiscreteNode("Y4", 2);
			idbn.addInterEdge("X", "X");
			idbn.addIntraEdge("X", "Y");
			idbn.addIntraEdge("X", "Y2");
			idbn.addIntraEdge("X", "Y3");
			idbn.addIntraEdge("X", "Y4");
			idbn.setInitialDistribution("X", piCPT);
			idbn.setDistribution("X", ACPT);
			idbn.setDistribution("Y", BCPT);
			idbn.setDistribution("Y2", BCPT);
			idbn.setDistribution("Y3", BCPT);
			idbn.setDistribution("Y4", BCPT);
			
			Integer[] obs = new Integer[8000];

			for(int i = 0; i < 8000; i++)
				obs[i] = i % 2;
			
			cdbn.setEvidence("Y", 0, obs);
			cdbn.setEvidence("Y", 0, obs);
			cdbn.setEvidence("Y2", 0, obs);
			cdbn.setEvidence("Y2", 0, obs);
			idbn.setEvidence("Y3", 0, obs);
			idbn.setEvidence("Y3", 0, obs);
			idbn.setEvidence("Y4", 0, obs);
			idbn.setEvidence("Y4", 0, obs);
			
			RunResults crr = cdbn.run_parallel_block(300, 0);
			RunResults irr = idbn.run_parallel_block(300, 0);
			
			System.out.println("Finished remote run in " + crr.timeElapsed + " (" + crr.numIts + " iterations, " + crr.error + " error) and local run in " + irr.timeElapsed + "(" + irr.numIts + " iterations, " + irr.error + " error).");
			
			for(int t = 0; t < 300; t++)
			{
				double[] val1 = new double[4];
				double[] diff = new double[4];
				double diffs = 0;
				for(int i = 0; i < 4; i++)
				{
					val1[i] = ((IDiscreteDynBayesNode)idbn.getNode("X")).getMarginal(t).getValue(i);
					diff[i] = val1[i];
					DiscreteMessage msg1 = (DiscreteMessage)cdbn.getMarginal("X", t);
					diff[i] -= msg1.getValue(i);
					diffs += Math.abs(diff[i]);
				}
				if(diffs!=0)
				{
					System.err.println("Difference at " + t + " of [" + diff[0] +", " + diff[1] + ", " + diff[2] + ", " + diff[3] );
				}
			}
			System.exit(0);
		}
		catch(Exception e) {
			System.err.println("Error " + e.getMessage());
		}
	}
}

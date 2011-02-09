package tests;

public class DBNFragmentTest {
/*
	public static void main(String[] args) throws BNException
	{
		double[][] A = {{.85, .05, .05, .05},{.05, .85, .05, .05},{.05, .05, .85, .05},{.05, .05, .05, .85}};
		double[][] B = {{.9, .1},{.1, .9},{.3, .7},{.7, .3}};
		double[] pi = {.8,.1, .05, .05};
		DiscreteCPT ACPT = new DiscreteCPT(new int[]{4}, 4, A);
		DiscreteCPT BCPT = new DiscreteCPT(new int[]{4}, 2, B);
		DiscreteCPTUC piCPT = new DiscreteCPTUC(pi);
		
		IDynNet idbn = BayesNetworkFactory.getDynamicNetwork(300);
		
		DBNFragment frag1 = new DBNFragment(99, FragmentSpot.Front);
		DBNFragment frag2 = new DBNFragment(99, FragmentSpot.Middle);
		DBNFragment frag3 = new DBNFragment(100, FragmentSpot.Back);
		
		DBNFragment sliver1 = new DBNFragment(1,FragmentSpot.Middle);
		DBNFragment sliver2 = new DBNFragment(1,FragmentSpot.Middle);
		
		DBNFragment[] frags = new DBNFragment[5];
		frags[0] = frag1; frags[1] = frag2; frags[2] = frag3;
		frags[3] = sliver1; frags[4] = sliver2;
		for(int i = 0; i < 5; i++)
		{
			frags[i].addDiscreteNode("X", 4);
			frags[i].addDiscreteNode("Y", 2);
			frags[i].addInterEdge("X", "X");
			frags[i].addIntraEdge("X", "Y");
			frags[i].setInitialDistribution("X", piCPT);
			frags[i].setDistribution("X", ACPT);
			frags[i].setDistribution("Y", BCPT);
		}

		idbn.addDiscreteNode("X", 4);
		idbn.addDiscreteNode("Y", 2);
		idbn.addInterEdge("X", "X");
		idbn.addIntraEdge("X", "Y");
		idbn.setInitialDistribution("X", piCPT);
		idbn.setDistribution("X", ACPT);
		idbn.setDistribution("Y", BCPT);
		Integer[] obs = new Integer[300];

		for(int i = 0; i < 300; i++)
		{
			obs[i] = i % 2;
		}
		idbn.setEvidence("Y", 0, obs);
		for(int i = 0; i < 99; i++)
			frags[0].setEvidence("Y", i, obs[i]);
		sliver1.setEvidence("Y", 0, obs[99]);
		for(int i = 100; i < 199; i++)
			frags[1].setEvidence("Y", i-100, obs[i]);
		sliver2.setEvidence("Y", 0, obs[199]);
		for(int i = 200; i < 300; i++)
			frags[2].setEvidence("Y", i-200, obs[i]);
		
		double totalErr = Double.MAX_VALUE;
		int it = 0;
		while(totalErr > 0)
		{
			it++;
			totalErr = 0;
			RunResults rr = frags[0].run(1, 0);
			totalErr += rr.error;
//			System.out.println("Fragment 1 finishes in " + rr.numIts + " iterations");
			rr = frags[1].run(1, 0);
			totalErr += rr.error;
//			System.out.println("Fragment 2 finishes in " + rr.numIts + " iterations");
			rr = frags[2].run(1, 0);
			totalErr += rr.error;
//			System.out.println("Fragment 3 finishes in " + rr.numIts + " iterations");
			
			sliver1.syncBwdIntf(frags[0].getFwdInterface());
			sliver1.syncFwdIntf(frags[1].getBwdInterface());
			
			sliver2.syncBwdIntf(frags[1].getFwdInterface());
			sliver2.syncFwdIntf(frags[2].getBwdInterface());
			
			totalErr += sliver1.run(1, 0).error;
			totalErr += sliver2.run(1, 0).error;
			
			frags[0].syncFwdIntf(sliver1.getBwdInterface());
			
			frags[1].syncBwdIntf(sliver1.getFwdInterface());
			frags[1].syncFwdIntf(sliver2.getBwdInterface());
			
			frags[2].syncBwdIntf(sliver2.getFwdInterface());
			
		}
		System.out.println("Finished in" + it + " iterations, fragmented");
		RunResults rr = idbn.run_parallel_block(300, 0);
		System.out.println("Finished in " + rr.numIts + " iterations, unfragmented");
		
		for(int t = 0; t < 300; t++)
		{
			DBNFragment frg; int tadj;
			if(t < 99)
			{
				frg = frags[0];
				tadj = t;
			}
			else if(t==99)
			{
				frg = sliver1;
				tadj = 0;
			}
			else if(t < 199)
			{
				frg = frags[1];
				tadj= t -100;
			}
			else if(t==199)
			{
				frg = sliver2;
				tadj = 0;
			}
			else
			{
				frg = frags[2];
				tadj = t-200;
			}
			double[] val1 = new double[4];
			double[] diff = new double[4];
			double diffs = 0;
			for(int i = 0; i < 4; i++)
			{
				val1[i] = ((IDynFDiscNode)idbn.getNode("X")).getMarginal(t).getValue(i);
				diff[i] = val1[i];
				diff[i] -= ((IDynFDiscNode)frg.getNode("X")).getMarginal(tadj).getValue(i);
				diffs += Math.abs(diff[i]);
			}
			if(diffs!=0)
			{
				System.err.println("Difference at " + t + " of [" + diff[0] +", " + diff[1] + ", " + diff[2] + ", " + diff[3] );
			}
		}
		
		
	}*/
}

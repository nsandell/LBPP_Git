package tests;

import bn.BNException;
import bn.IBayesNet.RunResults;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.SwitchingPoisson;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IInfDiscEvDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

public class PoissHMMTest {

	public static void main(String[] args) throws BNException
	{
		IDynamicBayesNet net = DynamicNetworkFactory.newDynamicBayesNet(500);
		IFDiscDBNNode x = net.addDiscreteNode("X", 2);
		int[] obs = new int[]{3, 1, 0, 7, 13, 6, 9, 9, 9, 10, 11, 5, 8, 13, 8, 10, 9, 6, 1, 0, 1, 2, 0, 0, 0, 1, 1, 0, 2, 4, 1, 2, 1, 0, 2, 0, 10, 11, 11, 13, 12, 7, 11, 12, 14, 5, 15, 9, 1, 0, 0, 1, 1, 0, 1, 1, 3, 1, 1, 3, 3, 1, 2, 0, 0, 3, 2, 1, 0, 1, 0, 0, 4, 1, 3, 0, 0, 0, 0, 3, 0, 1, 1, 0, 3, 2, 1, 2, 1, 2, 0, 2, 2, 0, 11, 0, 1, 2, 5, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 8, 9, 6, 8, 15, 2, 11, 8, 7, 12, 0, 2, 1, 0, 0, 1, 0, 2, 1, 2, 1, 1, 2, 1, 2, 8, 7, 10, 7, 7, 5, 11, 8, 11, 11, 0, 1, 1, 0, 0, 2, 0, 2, 1, 0, 1, 1, 9, 10, 11, 2, 10, 5, 14, 9, 12, 0, 0, 0, 3, 9, 13, 12, 6, 1, 1, 12, 15, 13, 12, 13, 3, 2, 3, 1, 1, 2, 12, 12, 12, 9, 7, 13, 12, 9, 10, 14, 2, 1, 2, 4, 1, 0, 2, 0, 1, 0, 13, 15, 10, 8, 9, 5, 11, 0, 1, 0, 0, 3, 1, 0, 2, 1, 1, 2, 1, 0, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 10, 7, 9, 10, 17, 5, 0, 3, 0, 16, 12, 8, 6, 13, 13, 9, 8, 6, 10, 4, 13, 8, 14, 8, 15, 17, 7, 9, 12, 8, 15, 15, 7, 0, 10, 11, 6, 9, 7, 10, 10, 0, 1, 10, 16, 10, 1, 1, 1, 0, 1, 2, 1, 1, 0, 1, 1, 0, 3, 1, 0, 7, 14, 15, 1, 3, 1, 0, 0, 1, 11, 0, 1, 11, 11, 10, 8, 8, 8, 6, 1, 0, 1, 1, 0, 2, 2, 0, 9, 6, 16, 2, 1, 2, 0, 13, 10, 6, 9, 14, 5, 9, 1, 1, 2, 1, 8, 12, 2, 0, 2, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 2, 1, 1, 0, 1, 5, 0, 1, 0, 0, 0, 1, 0, 1, 1, 2, 0, 0, 3, 2, 11, 13, 11, 1, 1, 0, 1, 14, 10, 14, 6, 2, 14, 6, 10, 8, 1, 2, 1, 1, 2, 11, 15, 12, 2, 1, 0, 2, 1, 1, 2, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 2, 10, 11, 9, 16, 5, 10, 11, 2, 2, 0, 1, 0, 1, 12, 11, 8, 9, 9, 11, 14, 8, 12, 9, 5, 0, 1, 0, 2, 1, 2, 0, 1, 2, 0, 0, 0, 10, 7, 14, 8, 13, 8, 10, 0, 1, 1, 0, 1, 13, 11, 5, 10, 11, 10, 0, 15, 13, 11, 13, 9, 9, 18, 11, 7, 10, 17, 5, 16, 11, 10, 0, 0, 0, 2, 0, 1, 1, 1, 0};
		IInfDiscEvDBNNode y = net.addDiscreteEvidenceNode("Y", obs);
		
		SwitchingPoisson dist = new SwitchingPoisson(new double[]{8, 6});
		x.setInitialDistribution(new DiscreteCPTUC(new double[]{.5, .5}));
		x.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}}, 2));
		y.setAdvanceDistribution(dist);
		net.addInterEdge(x, x);
		net.addIntraEdge(x, y);
		net.validate();

//		RunResults res = net.run_parallel_block(50, 0);
//		System.out.println("Run finished in " + res.numIts + " iterations with a final error of " + res.error);

//		net.run(1,0);
		
		RunResults res = net.optimize_parallel(20, .01, 20, .01);
		System.out.println("Finished in " + res.numIts + " iterations with a final error of " + res.error);
		
		for(int s = 0; s < 2; s++)
		{
			for(int i = 0; i < 500; i++)
				System.out.print(x.getMarginal(i).getValue(s) + " ");
			System.out.println();
		}
		dist.printDistribution(System.out);
	}
}

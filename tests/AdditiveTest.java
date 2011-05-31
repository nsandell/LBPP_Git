package tests;

import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.InhibitedSumOfPoisson;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IInfDiscEvDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;
import bn.messages.FiniteDiscreteMessage;

public class AdditiveTest {
	
	public static void main(String[] args) throws Exception
	{
		int[][] data = TwitterMFHMM.loadData("/Users/nsandell/Documents/MATLAB/additive_poisson_test/data");
		
		IDynamicBayesNet dbn = DynamicNetworkFactory.newDynamicBayesNet(1000);
		IInfDiscEvDBNNode y1 = dbn.addDiscreteEvidenceNode("y1", data[0]);
		IInfDiscEvDBNNode y2 = dbn.addDiscreteEvidenceNode("y2", data[1]);
		y1.setAdvanceDistribution(new InhibitedSumOfPoisson(new double[]{17.0,11.0}, 0.0, .1));
		y2.setAdvanceDistribution(new InhibitedSumOfPoisson(new double[]{1.0,1.0}, 0.0, .1));
		IFDiscDBNNode x1 = dbn.addDiscreteNode("x1", 2); 
		x1.setInitialDistribution(new DiscreteCPTUC(new double[]{1,0}));
		x1.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9, .1},{.1,.9}},2));
		IFDiscDBNNode x2 = dbn.addDiscreteNode("x2", 2);
		x2.setInitialDistribution(new DiscreteCPTUC(new double[]{1,0}));
		x2.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9, .1},{.1,.9}},2));
		
		dbn.addInterEdge(x1, x1);
		dbn.addInterEdge(x2, x2);
		dbn.addIntraEdge(x1, y1);
		dbn.addIntraEdge(x1, y2);
		dbn.addIntraEdge(x2, y1);
		dbn.addIntraEdge(x2, y2);

		dbn.optimize(5, 1e-6, 20, 1e-6);
		
		System.out.println(y1.getAdvanceDistribution().getDefinition());
		System.out.println(y2.getAdvanceDistribution().getDefinition());
		
		dbn.run(20,0);
	
		for(int i = 0; i < 1000; i++)
			System.out.print(((FiniteDiscreteMessage)x1.getMarginal(i)).getValue(1) + " ");
		System.out.println();
		for(int i = 0; i < 1000; i++)
			System.out.print(((FiniteDiscreteMessage)x2.getMarginal(i)).getValue(1) + " ");
	}

}

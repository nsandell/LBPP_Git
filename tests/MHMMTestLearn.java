package tests;

import java.util.Vector;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;
import complex.CMException;
import complex.latents.FiniteMarkovChain;
import complex.mixture.DirichletMixture;
import complex.mixture.IMixtureChild;
import complex.mixture.DirichletMixture.DMModelOptions;
import complex.mixture.controllers.MHMMController;

public class MHMMTestLearn {
	
	public static class BasicDiscreteChild extends IMixtureChild.MixtureSingleNodeChild
	{
		public BasicDiscreteChild(IFDiscDBNNode node)
		{
			super(node);
		}

		@Override
		public double parameterLL() {
			return 0;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws BNException, CMException {
		IDynamicBayesNet network = DynamicNetworkFactory.newDynamicBayesNet(36);
		
		int[][] o = new int[][]{
				{0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
				{0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
				{0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
				{0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
				{0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0}
		};
		
		Vector<BasicDiscreteChild> children = new Vector<BasicDiscreteChild>();
		for(int i = 0; i < o.length; i++)
		{
			IFDiscDBNNode nd = network.addDiscreteNode("Y"+i, 2);
			nd.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}},2));
			nd.setValue(o[i], 0);
			children.add(new BasicDiscreteChild(nd));
		}
		MHMMController controller = new MHMMController(network, children, 
				new FiniteMarkovChain.FiniteMarkovChainFactory(
						new FiniteMarkovChain.DiracFiniteMCPrior(	new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}}, 2),
																	new DiscreteCPTUC(new double[]{.9,.1}),2)));
		DMModelOptions opts = new DMModelOptions(controller, 1);
		opts.controller.setTrace(null);
		opts.controller.setLogger(System.out);
		opts.maxIterations = 50;
		DirichletMixture.learnDirichletMixture(opts);
	}

}

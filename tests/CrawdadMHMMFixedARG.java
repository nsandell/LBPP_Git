package tests;

import java.util.Collection;

import java.util.Vector;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;
import complex.IParentProcess;
import complex.mixture.DirichletMixture;
import complex.mixture.DirichletMixture.DMModelOptions;
import complex.mixture.controllers.MHMMController;
import complex.mixture.controllers.MHMMChild;
import complex.mixture.controllers.MHMMX;
import complex.mixture.controllers.MHMMController.MHMMParameterPrior;

public class CrawdadMHMMFixedARG {
	
	private static class Prior implements MHMMParameterPrior
	{
		
		public Prior() throws BNException
		{
			this.A = new DiscreteCPT(new double[][]{{.95,.05},{.1, .9}}, 2);
			this.pi = new DiscreteCPTUC(new double[]{.99,.01});
		}
		
		@Override
		public double evaluate(DiscreteCPT A) {
			return 1;
		}

		@Override
		public double evaluate(DiscreteCPTUC pi) {
			return 1;
		}

		@Override
		public DiscreteCPT initialSampleA() {
			try {
				return this.A.copy();
			} catch(BNException e) {
				System.err.println("Error sampling A matrix!");
				return null;
			}
		}

		@Override
		public DiscreteCPTUC initialSamplePi() {
			try {
				return this.pi.copy();
			} catch(BNException e) {
				System.err.println("Error sampling pi!");
				return null;
			}
		}

		@Override
		public DiscreteCPT posteriorSampleA(DiscreteCPT A,int T) {
			try {
				return this.A.copy();
			} catch(BNException e) {
				
				System.err.println("Error sampling A!");
				return null;
			}
		}

		@Override
		public DiscreteCPTUC posteriorSamplePi(DiscreteCPTUC pi) {
			return this.initialSamplePi();
		}
		
		DiscreteCPTUC pi;
		DiscreteCPT A;
	}
	
	private static class ARCrawTest implements MHMMChild
	{
		public ARCrawTest(IFDiscDBNNode node)
		{
			this.node = node;
		}
		IFDiscDBNNode node;
		
		public Collection<String> constituentNodeNames()
		{
			Vector<String> names = new Vector<String>();
			names.add(node.getName());
			return names;
		}
		
		@Override
		public String getName() {
			return node.getName();
		}

		@Override
		public double getDisagreement(int t) {
			return node.conditionalLL(t);
		}

		@Override
		public IDBNNode hook() {
			return node;
		}

		@Override
		public void optimize()
		{
			try {
				this.node.optimizeParameters();
			} catch(BNException e) {
				System.err.println("Failed to optimized node " + this.getName() + ": " + e.toString());
			}
		}

		@Override
		public void backupParameters() {}

		@Override
		public void restoreParameters() {}

		@Override
		public void setParent(IParentProcess rent) {}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		int[][] o = MFHMMLearnText.loadData(args[0]);
		int[][] ass = MFHMMLearnText.loadData(args[1]);
		String fileoutbase = args[2];
		if(ass.length != 1 || ass[0].length!=o.length)
			throw new Exception("Invalid assignment set.");
		IDynamicBayesNet network = DynamicNetworkFactory.newDynamicBayesNet(o[0].length);
		
		Vector<MHMMChild> children = new Vector<MHMMChild>();
		for(int i = 0; i < o.length; i++)
		{
			IFDiscDBNNode nd = network.addDiscreteNode("Y"+i, 2);
			network.addInterEdge(nd, nd);
			nd.setAdvanceDistribution(new DiscreteCPT(new int[]{2,2},2,new double[][]{{1-1e-9,1e-9},{1-1e-7,1e-7},{.5,.5},{.3,.7}}));
			nd.setInitialDistribution(new DiscreteCPT(new double[][]{{1-1e-9,1e-9},{.5,.5}},2));
			nd.setValue(o[i], 0);
			nd.lockParameters();
			children.add(new ARCrawTest(nd));
		}
		MHMMController controller = new MHMMController(network, children, new Prior(), 2, true);
		DMModelOptions<MHMMChild,MHMMX> opts = new DMModelOptions<MHMMChild, MHMMX>(controller, 1.0);
		opts.modelBaseName = fileoutbase;
		opts.controller.setLogger(System.out);
		opts.initialAssignment = ass[0];
		
		DirichletMixture.learnDirichletMixture(opts);
	}

}

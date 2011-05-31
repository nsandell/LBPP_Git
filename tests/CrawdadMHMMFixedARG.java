package tests;

import java.util.Collection;

import java.util.Vector;

import cern.jet.random.Beta;
import cern.jet.random.engine.DRand;

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
		DiscreteCPT Aback;
		DiscreteCPT piback;
		
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
		public void backupParameters()
		{
			try {
			this.Aback = (DiscreteCPT)this.node.getAdvanceDistribution().copy();
			this.piback = (DiscreteCPT)this.node.getInitialDistribution().copy();
			} catch(BNException e) 
			{
				System.err.println("ERROR : " + e.toString());
			}
		}

		@Override
		public void restoreParameters()
		{
			try {
			this.node.setAdvanceDistribution(this.Aback);
			this.node.setInitialDistribution(this.piback);
			} catch(BNException e)
			{
				System.err.println("ERROR " + e.toString());
			}
		}

		@Override
		public void setParent(IParentProcess rent) {}

		@Override
		public double parameterLL() {
			DiscreteCPT cpt = (DiscreteCPT)this.node.getAdvanceDistribution();
			Beta beta = new Beta(99, .1, new DRand());
			System.out.println(this.node.getName());
			System.out.println(cpt.values.length + " " + cpt.values[0].length);
			double ll = 0;
			ll += beta.pdf(cpt.values[0][0]);
			ll += beta.pdf(cpt.values[1][0]);
			beta.setState(1, 1);
			ll += beta.pdf(cpt.values[2][0]);
			beta.setState(1, 2);
			ll += beta.pdf(cpt.values[3][0]);
			return ll;
		}
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
			DiscreteCPT adva = new DiscreteCPT(new int[]{2,2},2,new double[][]{{1-1e-9,1e-9},{1-1e-7,1e-7},{.5,.5},{.3,.7}});
			adva.prior = new DiscreteCPT.CPTSufficient2SliceStat(adva);
			adva.prior.exp_tr[0][0] = 99;
			adva.prior.exp_tr[0][0] = .1;
			adva.prior.exp_tr[1][0] = 99;
			adva.prior.exp_tr[1][1] = .1;
			adva.prior.exp_tr[2][0] = 1;
			adva.prior.exp_tr[2][1] = 1;
			adva.prior.exp_tr[3][0] = 1;
			adva.prior.exp_tr[3][1] = 2;
			nd.setAdvanceDistribution(adva);
			nd.setInitialDistribution(new DiscreteCPT(new double[][]{{1-1e-9,1e-9},{.5,.5}},2));
			nd.setValue(o[i], 0);
			children.add(new ARCrawTest(nd));
		}
		MHMMController controller = new MHMMController(network, children, new Prior(), 2);
		DMModelOptions<MHMMChild,MHMMX> opts = new DMModelOptions<MHMMChild, MHMMX>(controller, 1.0);
		opts.modelBaseName = fileoutbase;
		opts.controller.setLogger(System.out);
		opts.initialAssignment = ass[0];
		
		DirichletMixture.learnDirichletMixture(opts);
	}

}

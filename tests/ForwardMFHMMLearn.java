package tests;

import java.io.File;

import java.util.Scanner;
import java.util.Vector;

import cern.jet.random.Beta;
import cern.jet.random.engine.DRand;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.TrueOr;
import bn.distributions.DiscreteCPT.CPTSufficient2SliceStat;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

import complex.IParentProcess;
import complex.featural.IBPMixture;
import complex.featural.IFeaturalChild;
import complex.featural.ProposalGenerator;
import complex.featural.IBPMixture.IBPMModelOptions;
import complex.featural.controllers.F2HMMX;
import complex.featural.controllers.MF2HMMController;
import complex.featural.controllers.MF2HMMController.MF2HMMInitialParamGenerator;
import complex.featural.proposal_generators.CoherenceAdder;
import complex.featural.proposal_generators.CoherenceSplitter;
import complex.featural.proposal_generators.CoherenceUniqueParenter;
import complex.featural.proposal_generators.SimilarityMerger;

public class ForwardMFHMMLearn {
	
	
	public static class ParamGen implements MF2HMMInitialParamGenerator
	{
		@Override
		public DiscreteCPT getInitialA() {
			try
			{
				return new DiscreteCPT(new double[][]{{.99,.01,0},{0,.9,.1},{0,0,1}},3);
			} catch(BNException e){return null;}
		}
		
		@Override
		public DiscreteCPTUC getInitialPi() {
			try
			{
				return new DiscreteCPTUC(new double[]{1,0,0});
			} catch(BNException e){return null;}
		}

		@Override
		public double getA_LL(DiscreteCPT A) {
			return 0;
		}

		@Override
		public double getPi_LL(DiscreteCPT pi) {
			return 0;
		}


		@Override
		public DiscreteCPT getInitialC() {
			try {
				return new DiscreteCPT(new double[][]{{1,0},{0,1},{1,0}}, 2);
			} catch(BNException e)
			{
				return null;
			}
		}

		@Override
		public double getC_LL(DiscreteCPT C) {
			return 0;
		}
	}
	
	public static class YSemiORWrapper implements IFeaturalChild
	{
		public YSemiORWrapper(String name,int[] o, IDynamicBayesNet net) throws BNException
		{
			this.y = net.addDiscreteNode(name, 2);
			this.y.setValue(o, 0);
			this.yor = net.addDiscreteNode(name+"_OR",2);
			net.addIntraEdge(this.yor, this.y);
			this.yor.setAdvanceDistribution(TrueOr.getInstance());
			double[][] distarr = new double[2][2];
			distarr[0][0] = this.beta.nextDouble(4, 1);
			distarr[0][1] = 1-distarr[0][0];
			distarr[1][1] = this.beta.nextDouble(4, 1);
			distarr[1][0] = 1-distarr[1][1];
			this.y.setAdvanceDistribution(new DiscreteCPT(distarr,2));
			DiscreteCPT dist = (DiscreteCPT)this.y.getAdvanceDistribution();
			CPTSufficient2SliceStat prior = new CPTSufficient2SliceStat(dist);
			prior.exp_tr[0][0] = 10; prior.exp_tr[0][1] = .5;
			prior.exp_tr[1][0] = 1; prior.exp_tr[1][1] = 4;
			dist.prior = prior;
		}
		
		DiscreteFiniteDistribution backupA;
		DiscreteFiniteDistribution backupPi;
		
		public void backupParameters() 
		{
			try {
				this.backupA = this.y.getAdvanceDistribution().copy();
			} catch(BNException e) {
				System.err.println("Failure to back up parameters..");
			}
		}
		public void restoreParameters()
		{
			try {
				this.y.setAdvanceDistribution(this.backupA);
			}
			catch(BNException e) {
				System.err.println("Failure to restore parameters...");
			}
		}
		
		public double getDisagreement(int t)
		{
			return this.y.conditionalLL(t);
		}
		
		public String getName()
		{
			return y.getName();
		}
		
		public IFDiscDBNNode hook()
		{
			return this.yor;
		}
		
		@Override
		public void addParent(IParentProcess p) {}

		@Override
		public void killParent(IParentProcess p) {}
			
		@Override
		public void optimize()
		{
			try {
				Vector<String> nodes = new  Vector<String>();
				nodes.add(yor.getName());
				nodes.add(y.getName());
				for(int i = 0; i < 10; i++)
				{
					yor.getNetwork().run(nodes, 5, 0);
					y.optimizeParameters();
				}
			} catch(BNException e) {
				System.err.println("Failure to optimize node " + y.getName());
			}
		}

		IFDiscDBNNode y, yor;

		@Override
		public double parameterLL() {
			try {
				DiscreteCPT dist = (DiscreteCPT)this.y.getAdvanceDistribution();
				double p00 = dist.evaluate(new int[]{0}, 0);
				double p11 = dist.evaluate(new int[]{1}, 1);

				double ll = 0;
				beta.setState(10,.5);
				ll += Math.log(beta.pdf(p00));
				beta.setState(4,1);
				ll += Math.log(beta.pdf(p11));
				return ll;
			} catch(BNException e) {
				System.err.println("Unable to evaluate distribution!");
				return Double.NaN;
			}
		}
		
		Beta beta = new Beta(1,1,new DRand());
	}
	

	public static void main(String[] args) throws Exception
	{
		Vector<IFeaturalChild> children = new Vector<IFeaturalChild>();
		IDynamicBayesNet net;
		boolean[][] ass;
		String out = null;
		if(args.length!=3)
		{
			System.out.println("using test data set...");
			net = DynamicNetworkFactory.newDynamicBayesNet(24);
			int[][] o = new int[][]{
					{0, 10, 11, 12,  0,  0,  0,  0,  0,  0,  0, 11, 11,  9,  0,  0,  0,  0,  0,  0,  0,  0, 10, 10},
					{0, 18, 19, 17,  0,  0,  0,  0,  0,  0,  0, 18, 19, 17,  0,  0,  0,  0,  0,  0,  0,  0, 18, 18},
					{0,  5,  6,  5,  0,  0,  0,  0,  0,  0,  0,  7,  4,  5,  0,  0,  0,  0,  0,  0,  0,  0,  6,  6},
					{0,  4,  5,  6,  0,  0, 10, 11, 12,  0,  0,  4,  5,  6,  0,  0, 10, 11, 12,  0,  0,  0, 16, 16},
					{0, 10, 11, 12,  0,  0,  4,  5,  6,  0,  0, 10, 11, 12,  0,  0,  5,  5,  6,  0,  0,  0, 16, 16},
					{0,100,101,104,  0,  0,  4,  5,  6,  0,  0,100,101,102,  0,  0,  4,  5,  6,  0,  0,  0,108,108},
					{0,  0,  0,  0,  0,  0,  9,  8,  9,  0,  0,  0,  0,  0,  0,  0,  9,  8,  9,  0,  0,  0,  8,  8},
					{0,  0,  0,  0,  0,  0,  3,  4,  3,  0,  0,  0,  0,  0,  0,  0,  3,  4,  5,  0,  0,  0,  4,  4},
					{0,  0,  0,  0,  0,  0,  9,  9,  9,  0,  0,  0,  0,  0,  0,  0,  9,  9,  9,  0,  0,  0,  9,  9}};
			for(int i = 0; i < 9; i++)
			{
				YSemiORWrapper child = new YSemiORWrapper("Y"+i, o[i], net);
				children.add(child);
			}
			ass = new boolean[9][0];
			ass = IBPMModelOptions.randomAssignment(o.length, 10, .2);
		}
		else
		{
			System.out.println("Loading external data...");
			String obs = args[0];
			out = args[1];
			String membership = args[2];

			int[][] o = loadData(obs);
			int[][] mi = loadData(membership);

			net = DynamicNetworkFactory.newDynamicBayesNet(o[0].length);
			for(int i = 0; i < o.length; i++)
			{
				YSemiORWrapper child = new YSemiORWrapper ("Y"+i, o[i], net);
				children.add(child);
			}
			ass = new boolean[o.length][mi[0].length];
			for(int i = 0; i < mi.length; i++)
				for(int j = 0; j < mi[0].length; j++)
					ass[i][j] = (mi[i][j]==1);
		}
		
		MF2HMMController cont = new MF2HMMController(net,children,new ParamGen(),2,3);
		Vector<ProposalGenerator<IFeaturalChild, F2HMMX>> gens = new Vector<ProposalGenerator<IFeaturalChild,F2HMMX>>();
		gens.add(new CoherenceSplitter<IFeaturalChild,F2HMMX>(1,1,1,1));
		gens.add(new SimilarityMerger<IFeaturalChild,F2HMMX>(1,1,1,1));
		gens.add(new CoherenceAdder<IFeaturalChild,F2HMMX>(1,1,1,1));
		gens.add(new CoherenceUniqueParenter<IFeaturalChild, F2HMMX>(1, 1, 1, 1));
		IBPMixture<IFeaturalChild,F2HMMX> mix = new IBPMixture<IFeaturalChild,F2HMMX>(gens,
				new double[]{.25,.25,.25,.25},
				new double[]{.15, .15, .7});
		cont.setLogger(System.out);

		IBPMModelOptions<IFeaturalChild,F2HMMX> opts = new IBPMModelOptions<IFeaturalChild,F2HMMX>(cont, ass);
		opts.maxIterations = 100;
		opts.alpha = 5;
		opts.temperature = 0;
		opts.learn_conv = 1e-5;
		opts.max_learn_it = 10;
		opts.run_conv = 1e-5;
		opts.max_run_it = 20;
		opts.savePath = out;
		
		opts.finalize = true;
		opts.max_finalize_iterations = 4;
		mix.learn(opts);
	}
	
	public static int[][] loadData(String file) throws Exception
	{
		Scanner scan = new Scanner(new File(file));
		int rows = scan.nextInt();
		int cols = scan.nextInt();
		int[][] dat = new int[rows][cols];
		for(int i = 0; i < rows; i++)
		{
			for(int j = 0; j < cols; j++)
			{
				if(!scan.hasNext())
					throw new BNException("Failed to read data, less elements than specified!");
				dat[i][j] = scan.nextInt();
			}
		}
		return dat;
	}
}

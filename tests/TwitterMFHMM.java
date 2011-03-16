package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

import util.MathUtil;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.SumOfPoisson;
import bn.dynamic.IDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IInfDiscEvDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

import complex.IParentProcess;
import complex.featural.IBPMixture;
import complex.featural.IFeaturalChild;
import complex.featural.ProposalGenerator;
import complex.featural.IBPMixture.IBPMModelOptions;
import complex.featural.controllers.FHMMX;
import complex.featural.controllers.MFHMMController;
import complex.featural.controllers.MFHMMController.MFHMMInitialParamGenerator;
import complex.featural.proposal_generators.CoherenceAdder;
import complex.featural.proposal_generators.CoherenceSplitter;
import complex.featural.proposal_generators.RandomAbsorbGenerator;
import complex.featural.proposal_generators.RandomExpungeGenerator;
import complex.featural.proposal_generators.RandomMergeGenerator;
import complex.featural.proposal_generators.RandomSplitGenerator;
import complex.featural.proposal_generators.SimilarityMerger;

public class TwitterMFHMM {
	
	public static class YTwitterWrapper implements IFeaturalChild
	{
		public YTwitterWrapper(String name, int[] values, IDynamicBayesNet net) throws BNException
		{
			this.y = net.addDiscreteEvidenceNode(name, values);
			this.y.setAdvanceDistribution(new SumOfPoisson(new double[]{}));
			this.sop = (SumOfPoisson)this.y.getAdvanceDistribution();
		}
		
		public void backupParameters(){
			try {
				backup = sop.copy();
			} catch(BNException e) {
				System.err.println(e.toString());
			}
		}
		public void restoreParameters(){
			if(backup!=null)
			{
				try {
					this.y.setAdvanceDistribution(backup);
					this.sop = (SumOfPoisson)this.y.getAdvanceDistribution();
				} catch(BNException e) {
					System.err.println(e.toString());
				}
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
		
		public IDBNNode hook()
		{
			return this.y;
		}

		@Override
		public void addParent(IParentProcess p) {
			this.rents.add(p);
			this.sop.newParent(MathUtil.rand.nextDouble()*15);
		}

		@Override
		public void killParent(IParentProcess p) {
			int index = -1;
			for(int i = 0; i < rents.size(); i++)
				if(rents.get(i).equals(p))
					index = i;
			try {
				if(index!=-1)
				{
					this.rents.remove(index);
					this.sop.killParent(index);
				}
				else
					System.err.println("Attempted to remove parent that was not attached.");
			} catch(BNException e) {
				System.err.println("Attempted to remove parent that was not attached.");
			}
		}
		
		@Override
		public void optimize()
		{
			Vector<String> thisv = new Vector<String>();
			thisv.add(this.y.getName());
			try {
				this.y.getNetwork().run(thisv, 10, 1e-8);
				this.y.optimizeParameters();
			} catch(BNException e) 
			{
				System.err.println("Failed to optimize node " + this.y.getName() + ", " + e.toString());
			}
		}

		ArrayList<IParentProcess> rents = new ArrayList<IParentProcess>();
		SumOfPoisson sop, backup = null;
		IInfDiscEvDBNNode y;
	}
	
	public static class ParamGen implements MFHMMInitialParamGenerator
	{
		@Override
		public DiscreteCPT getInitialA() {
			try
			{
				return new DiscreteCPT(new double[][]{{.7,.3},{.3,.7}}, 2);
			} catch(BNException e){return null;}
		}
		
		@Override
		public DiscreteCPTUC getInitialPi() {
			try
			{
				return new DiscreteCPTUC(new double[]{.7,.3});
			} catch(BNException e){return null;}
		}
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
			net = DynamicNetworkFactory.newDynamicBayesNet(20);
			int[][] o = new int[][]{{0,10,11,12,0,0,0,0,0,0,0,11,11,9,0,0,0,0,0,0},
					{0,18,19,17,0,0,0,0,0,0,0,18,19,17,0,0,0,0,0,0},
					{0,5,6,5,0,0,0,0,0,0,0,7,4,5,0,0,0,0,0,0},
					{0,4,5,6,0,0,10,11,12,0,0,4,5,6,0,0,10,11,12,0},
					{0,10,11,12,0,0,4,5,6,0,0,10,11,12,0,0,5,5,6,0},
					{0,100,101,104,0,0,4,5,6,0,0,100,101,102,0,0,4,5,6,0},
					{0,0,0,0,0,0,9,8,9,0,0,0,0,0,0,0,9,8,9,0},
					{0,0,0,0,0,0,3,4,3,0,0,0,0,0,0,0,3,4,5,0},
					{0,0,0,0,0,0,9,9,9,0,0,0,0,0,0,0,9,9,9,0}};
			for(int i = 0; i < 9; i++)
			{
				YTwitterWrapper child = new YTwitterWrapper("Y"+i, o[i], net);
				children.add(child);
			}
			ass = new boolean[9][0];
			/*ass = new boolean[9][1];
			for(int i = 0; i < 9; i++)
				ass[i][0] = true;
			/*ass = new boolean[][] {
				{true,false,false},
				{true,false,false},
				{true,false,false},
				{true,false,false},
				{true,false,false},
				{true,false,false},
				{true,true,false},
				{true,true,true},
				{true,true,true}
			};*/
			//ass = IBPMModelOptions.randomAssignment(o.length, 20, .2);
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
				YTwitterWrapper child = new YTwitterWrapper("Y"+i, o[i], net);
				children.add(child);
			}
			ass = new boolean[o.length][mi[0].length];
			for(int i = 0; i < mi.length; i++)
				for(int j = 0; j < mi[0].length; j++)
					ass[i][j] = (mi[i][j]==1);
		}
		
		MFHMMController cont = new MFHMMController(net,children,new ParamGen(),2);
		Vector<ProposalGenerator<IFeaturalChild, FHMMX>> gens = new Vector<ProposalGenerator<IFeaturalChild,FHMMX>>();
		gens.add(new RandomAbsorbGenerator<IFeaturalChild,FHMMX>(.05, .05));
		gens.add(new RandomExpungeGenerator<IFeaturalChild,FHMMX>(.05, .05));
		gens.add(new RandomMergeGenerator<IFeaturalChild,FHMMX>(.1, .1));
		gens.add(new RandomSplitGenerator<IFeaturalChild,FHMMX>(.1, .1));
		gens.add(new CoherenceSplitter<IFeaturalChild,FHMMX>(.35, .1, .1, .35));
		gens.add(new SimilarityMerger<IFeaturalChild,FHMMX>(.35, .1, .35, .1));
		gens.add(new CoherenceAdder<IFeaturalChild,FHMMX>(.35, .1, .1, .35));
		IBPMixture<IFeaturalChild,FHMMX> mix = new IBPMixture<IFeaturalChild,FHMMX>(gens,
				new double[] {.05,.05,.1,.1,.2,.15,.35},
				new double[]{.7, 0, .3});
		cont.setLogger(System.out);

		IBPMModelOptions<IFeaturalChild,FHMMX> opts = new IBPMModelOptions<IFeaturalChild,FHMMX>(cont, ass);
		opts.maxIterations = 50000; 
		opts.alpha = 1;
		opts.learn_conv = .01;
		opts.max_learn_it = 5;
		opts.run_conv = 1e-5;
		opts.max_run_it = 20;
		opts.savePath = "/Users/nsandell/tmp/";//out;
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

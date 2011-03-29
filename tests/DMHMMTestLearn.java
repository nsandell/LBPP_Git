package tests;

import java.util.Collection;

import java.util.Iterator;

import java.util.Vector;

import util.MathUtil;

import cern.jet.random.Beta;
import cern.jet.random.engine.DRand;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;
import complex.IParentProcess;
import complex.mixture.DirichletMixture;
import complex.mixture.DirichletMixture.DMModelOptions;
import complex.mixture.controllers.MHMMController;
import complex.mixture.controllers.MHMMChild;
import complex.mixture.controllers.MHMMX;
import complex.mixture.controllers.MHMMController.MHMMParameterPrior;

public class DMHMMTestLearn {
	
	private static class BetaPrior implements MHMMParameterPrior
	{
		
		public BetaPrior(double base, double stbias)
		{
			this.alpha = base+stbias;
			this.beta =  base;
			this.betadist = new Beta(base+stbias,base, new DRand(MathUtil.rand.nextInt()));
		}

		@Override
		public double evaluate(DiscreteCPT A) {
			try {
				double ll = 0;
				ll += betadist.pdf(A.evaluate(new int[]{0}, 0));
				ll += betadist.pdf(A.evaluate(new int[]{1}, 1));
				return ll;
			} catch(BNException e) {
				System.err.println("Error evluating CPT");
				return Double.NaN;
			}
		}

		@Override
		public double evaluate(DiscreteCPTUC pi) {
			return 1;
		}

		@Override
		public DiscreteCPT initialSampleA() {
			try {
				double a11 = betadist.nextDouble();
				double a22 = betadist.nextDouble();
				return new DiscreteCPT(new double[][]{{a11,1-a11},{1-a22,a22}}, 2);
			} catch(BNException e) {
				System.err.println("Error sampling A matrix!");
				return null;
			}
		}

		@Override
		public DiscreteCPTUC initialSamplePi() {
			try {
				return new DiscreteCPTUC(new double[]{.99,.01});
			} catch(BNException e) {
				System.err.println("Error sampling pi!");
				return null;
			}
		}

		@Override
		public DiscreteCPT posteriorSampleA(DiscreteCPT A,int T) {
			try {
				double a11 = betadist.nextDouble(this.alpha+A.evaluate(new int[]{0},0)*T,this.beta+A.evaluate(new int[]{0}, 1));
				double a22 = betadist.nextDouble(this.alpha+A.evaluate(new int[]{1},1)*T,this.beta+A.evaluate(new int[]{1}, 0));
				return new DiscreteCPT(new double[][]{{a11,1-a11},{1-a22,a11}}, 2);
			} catch(BNException e) {
				System.err.println("Error sampling A matrix!");
				return null;
			}
		}

		@Override
		public DiscreteCPTUC posteriorSamplePi(DiscreteCPTUC pi) {
			return pi;
		}
	
		double alpha, beta;
		Beta betadist;
	}
	
	private static class SingularFDMS implements MessageSet<FiniteDiscreteMessage>
	{
		public SingularFDMS(FiniteDiscreteMessage sing) {
			this.msg = sing;
		}
		
		private static class SingIt implements Iterator<FiniteDiscreteMessage>
		{
			public SingIt(FiniteDiscreteMessage msg)
			{
				this.msg = msg;
			}
			FiniteDiscreteMessage msg;

			@Override
			public boolean hasNext() {
				return msg!=null;
			}

			@Override
			public FiniteDiscreteMessage next() {
				FiniteDiscreteMessage tmp = msg;
				msg = null;
				return tmp;
			}

			@Override
			public void remove() throws UnsupportedOperationException {throw new UnsupportedOperationException();}
		}
		
		private FiniteDiscreteMessage msg;

		@Override
		public Iterator<FiniteDiscreteMessage> iterator() {
			return new SingIt(msg);
		}

		@Override
		public int size() {
			return this.msg==null ? 0 : 1;
		}

		@Override
		public FiniteDiscreteMessage get(int index) {
			return index==0 ? msg : null; 
		}

		@Override
		public void remove(int index) {
			this.msg = index==0 ? null : this.msg;
		}

		@Override
		public void removeAll() {
			this.msg = null;
		}
	}
	
	private static class MHMMCTest implements MHMMChild
	{
		public MHMMCTest(IFDiscDBNNode node)
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
		IDynamicBayesNet network = DynamicNetworkFactory.newDynamicBayesNet(o[0].length);
		
		Vector<MHMMChild> children = new Vector<MHMMChild>();
		for(int i = 0; i < o.length; i++)
		{
			IFDiscDBNNode nd = network.addDiscreteNode("Y"+i, 2);
			nd.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}},2));
			nd.setValue(o[i], 0);
			children.add(new MHMMCTest(nd));
		}
		MHMMController controller = new MHMMController(network, children, new BetaPrior(1.0, 2.0), 2);
		DMModelOptions<MHMMChild,MHMMX> opts = new DMModelOptions<MHMMChild, MHMMX>(controller, 1.0);
		opts.controller.setLogger(System.out);
		

		//opts.initialAssignment = new int[]{0,0,0,0,1,2,2,2,2,2,3,3,3,3,3,4,4,4,4,4,5,5,5,5,5,6,6,6,6,6};
		
		DirichletMixture.learnDirichletMixture(opts);
	}

}

package tests;

import java.util.Collection;

import java.util.Iterator;

import java.util.Vector;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;
import complex.CMException;
import complex.IParentProcess;
import complex.mixture.FixedMixture;
import complex.mixture.FixedMixture.FMModelOptions;
import complex.mixture.controllers.MHMMController;
import complex.mixture.controllers.MHMMChild;
import complex.mixture.controllers.MHMMX;
import complex.mixture.controllers.MHMMController.MHMMParameterPrior;

public class MHMMTestLearn {
	
	private static class Priors implements MHMMParameterPrior
	{

		@Override
		public double evaluate(DiscreteCPT A) {
			return 1;
		}

		@Override
		public double evaluate(DiscreteCPTUC pi) {
			// TODO Auto-generated method stub
			return 1;
		}

		@Override
		public DiscreteCPT initialSampleA() {
			try {
				return new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}}, 2);
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
		public DiscreteCPT posteriorSampleA(DiscreteCPT A, int T) {
			return A;
		}

		@Override
		public DiscreteCPTUC posteriorSamplePi(DiscreteCPTUC pi) {
			return pi;
		}
		
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

		@Override
		public double parameterLL() {
			// TODO Auto-generated method stub
			return 0;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws BNException, CMException {
		IDynamicBayesNet network = DynamicNetworkFactory.newDynamicBayesNet(600);
		
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
				{0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0}
		};
		
		Vector<MHMMChild> children = new Vector<MHMMChild>();
		for(int i = 0; i < o.length; i++)
		{
			IFDiscDBNNode nd = network.addDiscreteNode("Y"+i, 2);
			nd.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}},2));
			nd.setValue(o[i], 0);
			children.add(new MHMMCTest(nd));
		}
		MHMMController controller = new MHMMController(network, children, new Priors(), 2);
		FMModelOptions<MHMMChild,MHMMX> opts = new FMModelOptions<MHMMChild,MHMMX>(controller,6);
		opts.controller.setLogger(System.out);
		FixedMixture.learnFixedMixture(opts);
	}

}

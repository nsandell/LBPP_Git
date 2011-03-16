package complex.prepacked;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import util.MathUtil;

import bn.BNException;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.distributions.DiscreteDistribution.InfiniteDiscreteDistribution;
import bn.distributions.DiscreteCPT;
import bn.distributions.Distribution;
import bn.distributions.SwitchingPoisson;
import bn.dynamic.IDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IInfDiscEvDBNNode;
import bn.messages.FiniteDiscreteMessage;

import complex.CMException;
import complex.featural.IParentProcess;
import complex.mixture.controllers.MHMMController.MHMMChild;
import complex.prepacked.MHMM.MHMMChildFactory;

public class MHMMPoisson
{
	
	private static class ARCFactory implements MHMMChildFactory
	{
		public void setArg(String arg) throws CMException
		{
			this.nsar = Integer.parseInt(arg);
		}
		int nsar = 2;
		
		@Override
		public MHMMChild getChild(IDynamicBayesNet net, int nameidx, int ns,
				int[] observations) {
			try {
				return new HiddenARPoiss(net, nameidx, ns, nsar, observations);
			} catch(BNException e) {
				System.err.println("");
				return null;
			}
		}
	}
	
	private static class BPCFactory implements MHMMChildFactory
	{

		@Override
		public MHMMChild getChild(IDynamicBayesNet net, int nameidx, int ns,
				int[] observations) {
			try {
				IInfDiscEvDBNNode nd = net.addDiscreteEvidenceNode("Y"+nameidx, observations);
				double[] means = new double[ns];
				for(int i = 0; i < ns; i++)
					means[i] = 30.0*MathUtil.rand.nextDouble();
				SwitchingPoisson dist = new SwitchingPoisson(means);
				nd.setAdvanceDistribution(dist);
				return new BasicPoissChild(nd);
			} catch(BNException e) {
				System.err.println("Error creating child Y"+nameidx+": " + e.toString());
				return null;
			}
		}

		@Override
		public void setArg(String arg) throws CMException {
		}
		
	}
	
	private static class TCFactory implements MHMMChildFactory
	{

		@Override
		public MHMMChild getChild(IDynamicBayesNet net, int nameidx, int ns,
				int[] observations) {
			try {
				return new Twitter(net, nameidx, observations);
			} catch(BNException e) {
				System.err.println("Error creating child Y"+nameidx+": " + e.toString());
				return null;
			}
		}

		@Override
		public void setArg(String arg) throws CMException {
		}
		
	}
	
	private static class Twitter implements MHMMChild
	{
		public Twitter(IDynamicBayesNet net, int id, int[] obs) throws BNException
		{
			this.evnode = net.addDiscreteEvidenceNode("YEV"+id, obs);
			SwitchingPoisson dist = new SwitchingPoisson(new double[]{0.0,0.0,1.0,10.0},new int[]{2,2});
			dist.lockMean(new int[]{0,0}, true);
			dist.lockMean(new int[]{0,1}, true);
			this.evnode.setAdvanceDistribution(dist);
			
			this.state = net.addDiscreteNode("YS"+id, 2);
			this.state.setAdvanceDistribution(new DiscreteCPTUC(new double[]{.5,.5}));

			this.net = net;
			
			net.addIntraEdge(this.state, this.evnode);
			
			nodeNames.add("YEV"+id);
			nodeNames.add("YS"+id);
			this.name = "Y"+id;
		}
		
		private Vector<String> nodeNames = new Vector<String>();
		private IDynamicBayesNet net;
		private IFDiscDBNNode state;
		private IInfDiscEvDBNNode evnode;
		private String name;
		
		@Override
		public String getName() {
			return this.name;
		}
		
		InfiniteDiscreteDistribution poissBackup = null;
		DiscreteFiniteDistribution stateBackup = null;
		@Override
		public void backupParameters() throws CMException {
			try {
				this.poissBackup = evnode.getAdvanceDistribution().copy();
				this.stateBackup = state.getAdvanceDistribution().copy();
			} catch(BNException e) {
				throw new CMException("Error backing up parameters.. this shouldn't happen: " + e.toString());
			}
		}
		@Override
		public void restoreParameters() throws CMException {
			try {
				this.evnode.setAdvanceDistribution(this.poissBackup);
				this.state.setAdvanceDistribution(this.stateBackup);
			} catch(BNException e) {
				throw new CMException("Error storing parameters.. this shouldn't happen: " + e.toString());
			}
		}
		@Override
		public double getDisagreement(int t) {
			return 0;
		}
		@Override
		public IDBNNode hook() {
			return this.evnode;
		}
		@Override
		public void optimize(Vector<FiniteDiscreteMessage> incPis)
		{
			try {

				this.net.run(this.nodeNames, 10, 1e-8);
				this.state.optimizeParameters();
				this.evnode.optimizeParameters();
				this.net.run(this.nodeNames, 10, 1e-8);
				this.state.optimizeParameters();
				this.evnode.optimizeParameters();
				
			} catch(BNException e) {
				System.err.println("Failed to optimized for node " + this.getName());
			}
		}
		@Override
		public double evaluateP() {
			return 1;
		}
		@Override
		public void sampleInit() {}
		@Override
		public void samplePosterior() {}
		@Override
		public Collection<String> constituentNodeNames() {
			return this.nodeNames;
		}
		
		@Override
		public void addParent(IParentProcess parent) {}

		@Override
		public void killParent(IParentProcess parent) {}

		@Override
		public void optimize() {}
	}
	
	
	//TODO This isn't a well thought out child.  Individual chains might as well be responsible
	// for observations as much as the shared chains!
	private static class HiddenARPoiss implements MHMMChild
	{
		public HiddenARPoiss(IDynamicBayesNet net, int id, int nsx, int nsar, int[] observations) throws BNException
		{
			this.evnode = net.addDiscreteEvidenceNode("YEV"+id, observations);
			double[] means = new double[nsar*nsx];
			for(int i = 0; i < means.length; i++)
				means[i] = 30.0*MathUtil.rand.nextDouble();
			this.evnode.setAdvanceDistribution(new SwitchingPoisson(means,new int[]{2,2}));
			this.arnode = net.addDiscreteNode("YAR"+id, nsar);
			//TODO Need to think of a better what of configuring these parameters...
			//TODO a good idea would be to take a configuration file, if none provided go
			// through a manual prompt
			if(nsar!=2 || nsx!=2)
				throw new BNException("Hey duder you need to implement more flexible parameters if you want to do this.");
			DiscreteCPT a = new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}},2);
			DiscreteCPTUC pi = new DiscreteCPTUC(new double[]{.5,.5});
			this.arnode.setAdvanceDistribution(a);
			this.arnode.setInitialDistribution(pi);
			net.addInterEdge(this.arnode, this.arnode);
			net.addIntraEdge(this.arnode, this.evnode);
			this.name = "Y"+id;
			this.net = net;
			this.nnset = new Vector<String>();
			this.nnset.add(this.evnode.getName());
			this.nnset.add(this.arnode.getName());
		}
		Vector<String> nnset;
		IDynamicBayesNet net;
		IInfDiscEvDBNNode evnode;
		IFDiscDBNNode arnode;
		String name;
		
		@Override
		public String getName() {
			return name;
		}

		InfiniteDiscreteDistribution poiss_backup;
		DiscreteFiniteDistribution a_dist, b_dist;
		@Override
		public void backupParameters() throws CMException {
			try {
				this.poiss_backup = this.evnode.getAdvanceDistribution().copy();
				this.a_dist = this.arnode.getAdvanceDistribution().copy();
				this.b_dist = this.arnode.getInitialDistribution().copy();
			} catch(BNException e) {
				System.err.println("Error while backing up parameters.. this shouldn't happen : " + e.toString());
			}
		}
		
		@Override
		public void restoreParameters() throws CMException {
			if(this.a_dist==null || this.b_dist==null || this.poiss_backup==null)
				return;
			try {
				this.evnode.setAdvanceDistribution(this.poiss_backup);
				this.arnode.setAdvanceDistribution(this.a_dist);
				this.arnode.setInitialDistribution(this.b_dist);
			} catch(BNException e) {
				throw new CMException("Failure to restore ARPoiss paramters - this should not happen!\n"+e.toString());
			}
		}

		@Override
		public double getDisagreement(int t) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public IDBNNode hook() {
			return this.evnode;
		}
		
		@Override
		public void optimize(Vector<FiniteDiscreteMessage> incPis) {
			try {
				this.net.run(this.nnset, 20, 1e-8);
				
				this.arnode.optimizeParameters();
				this.evnode.optimizeParameters();
				
			} catch(BNException e) {
				System.err.println("Error while attempting to optimize observable" + this.name);
			}
		}
		
		@Override
		public double evaluateP() {
			return 1;
		}
		
		@Override
		public void sampleInit() {}
		
		@Override
		public void samplePosterior() {}
		
		@Override
		public void addParent(IParentProcess parent) {}

		@Override
		public void killParent(IParentProcess parent) {}

		@Override
		public void optimize() {}
		
		@Override
		public Collection<String> constituentNodeNames() {
			return this.nnset;
		}
	}
	
	private static class BasicPoissChild implements MHMMChild
	{
		public BasicPoissChild(IInfDiscEvDBNNode node)
		{
			this.node = node;
		}
		private IInfDiscEvDBNNode node;
		
		@Override
		public String getName() {
			return node.getName();
		}

		@Override
		public double getDisagreement(int t) {
			return node.conditionalLL(t);
		}
		
		public Collection<String> constituentNodeNames()
		{
			Vector<String> names = new Vector<String>();
			names.add(node.getName());
			return names;
		}

		@Override
		public IDBNNode hook() {
			return node;
		}

		@Override
		public void optimize(Vector<FiniteDiscreteMessage> chainIncPis)
		{
			try {
				int N = chainIncPis.get(0).getCardinality();
				
				double[] sums = new double[N];
				double[] times = new double[N];
				int T = this.node.getNetwork().getT();
				for(int t = 0; t < T; t++) {
					for(int i = 0; i < N; i++) {
						sums[i] += ((double)this.node.getValue(t))*chainIncPis.get(t).getValue(i);
						times[i] += chainIncPis.get(t).getValue(i);
					}
				}
				
				double[] means = new double[N];
				for(int i = 0; i < N; i++)
				{
					if(times[i]==0)
						continue;
					means[i] = sums[i]/times[i];
				}

				this.node.setAdvanceDistribution(new SwitchingPoisson(means));
			} catch(BNException e) {
				System.err.println("Failed to optimize node " + this.getName() + " : " + e.toString());
			}
		}

		@Override
		public double evaluateP() {
			return 1;
		}

		@Override
		public void sampleInit() {}

		@Override
		public void samplePosterior() {}

		@Override
		public void backupParameters() throws CMException{
			try {
				this.backupDist = this.node.getAdvanceDistribution().copy();
			} catch(BNException e) {
				throw new CMException("Failed to backup distribution for node " + this.getName() + " : " + e.toString());
			}
		}

		@Override
		public void restoreParameters() throws CMException {
			if(this.backupDist!=null)
			{
				try {
					this.node.setAdvanceDistribution(this.backupDist);
				} catch(BNException e) {
					throw new CMException("Failed to backup distribution for node " + this.getName() + " : " + e.toString());
				}
			}
		}
		
		@Override
		public void addParent(IParentProcess parent) {}

		@Override
		public void killParent(IParentProcess parent) {}

		@Override
		public void optimize() {}
		
		Distribution backupDist = null;
		
	}

	public static void main(String[] args) throws BNException, CMException
	{
		HashMap<String,MHMMChildFactory> factories = new HashMap<String,MHMMChildFactory>();
		factories.put("default",new BPCFactory());
		factories.put("basic", new BPCFactory());
		factories.put("ar", new ARCFactory());
		factories.put("twit", new TCFactory());
		MHMM.mhmm_main(args, factories);
	}
}

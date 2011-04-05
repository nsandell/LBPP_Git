package complex.prepacked;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.Distribution;
import bn.dynamic.IDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import complex.CMException;
import complex.IParentProcess;
import complex.mixture.controllers.MHMMChild;
import complex.prepacked.MHMM.MHMMChildFactory;

public class MHMMDiscrete
{
	
	private static class BDCFactory implements MHMMChildFactory
	{

		@Override
		public MHMMChild getChild(IDynamicBayesNet net, int nameIndex, int ns, int[] observations)
		{
			try {
				int T = net.getT();
				int cardinality = 1;
				for(int j = 0; j < T; j++)
					cardinality = Math.max(cardinality, observations[j]+1);
				IFDiscDBNNode nd = net.addDiscreteNode("Y"+nameIndex, cardinality);
				nd.setAdvanceDistribution(new DiscreteCPT(obsmat(ns, cardinality),cardinality));
				nd.setValue(observations, 0);
				return new BasicDiscreteChild(nd);
			} catch(BNException bne) {
				System.err.println("Error creating observation node Y"+nameIndex+": " + bne.toString());
				return null;
			}
		}
		
		private static double[][] obsmat(int ns, int no)
		{
			double[][] mat = new double[ns][no];
			for(int i = 0; i < ns; i++)
			{
				for(int j = 0; j < no; j++)
				{
					if(i%no==j)
						mat[i][j] = concentration;
					else
						mat[i][j] = (1-concentration)/(no-1);
				}
			}
			return mat;
		}
		
		static double concentration = .9;

		@Override
		public void setArg(String arg) throws CMException {
			try {
				concentration = Double.parseDouble(arg);
				if(concentration < 0 || concentration > 1)
					throw new CMException("Concentration parameter for discrete nodes must be in range [0,1]");
			} catch(NumberFormatException e) {
				throw new CMException("Error parsing floating point concentration parameter : " + arg);
			} 
		}
	}
	
	private static class BasicDiscreteChild implements MHMMChild
	{
		public BasicDiscreteChild(IFDiscDBNNode node)
		{
			this.node = node;
		}
		private IFDiscDBNNode node;
		
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
		public void optimize()
		{
			try {
				this.node.optimizeParameters();
			} catch(BNException e) {
				System.err.println("Failed to optimize node " + this.getName());
			}
		}

		@Override
		public void setParent(IParentProcess rent) {}

		@Override
		public void backupParameters() throws CMException {
			try {
				this.backupDist = this.node.getAdvanceDistribution().copy();
			} catch(BNException e) {
				throw new CMException("Failed to backup parameters for node " + this.getName() + ": " + e.getMessage());
			}
		}

		@Override
		public void restoreParameters() throws CMException {
			if(backupDist!=null)
			{
				try {
					this.node.setAdvanceDistribution(backupDist);
				} catch(BNException e) {
				throw new CMException("Failed to restore parameters for node " + this.getName() + ": " + e.getMessage());
			}
			}
		}
		Distribution backupDist = null;

		@Override
		public double parameterLL() {
			return 0;
		}

	}

	public static void main(String[] args) throws BNException, CMException
	{
		HashMap<String,MHMMChildFactory> factories = new HashMap<String,MHMMChildFactory>();
		factories.put("default",new BDCFactory());
		factories.put("basic", new BDCFactory());
		MHMM.mhmm_main(args, factories);
	}
}

package complex;

import java.util.Collection;
import java.util.Vector;

import bn.BNException;
import bn.distributions.Distribution;
import bn.dynamic.IDBNNode;
import complex.metrics.Coherence.DisagreementMeasure;

public interface IChildProcess extends DisagreementMeasure{

	String getName();
	
	double parameterLL();

	void backupParameters() throws CMException;
	void restoreParameters() throws CMException;
	void optimize();
	
	IDBNNode hook();

	Collection<String> constituentNodeNames();
	
	public static abstract class SingleNodeChildProcess implements IChildProcess
	{
		public SingleNodeChildProcess(IDBNNode node)
		{
			this.node = node;
			this.constColl.add(node.getName());
		}
		
		@Override
		public String getName(){return this.node.getName();}
	
		@Override
		public double getDisagreement(int t){return this.node.conditionalLL(t);}
	
		@Override
		public IDBNNode hook(){return this.node;};
	
		@Override
		public Collection<String> constituentNodeNames()
		{
			return this.constColl;
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
		public void backupParameters() throws CMException {
			try {
				if(this.initial_backup!=null)
					this.initial_backup = this.node.getInitialDistribution().copy();
				if(this.advance_backup!=null)
					this.advance_backup = this.node.getAdvanceDistribution().copy();
			} catch(BNException e) {
				throw new CMException("Failed to backup parameters for node " + this.getName() + ": " + e.getMessage());
			}
		}

		@Override
		public void restoreParameters() throws CMException {
						
			try {
				if(this.initial_backup!=null)
					this.node.setInitialDistribution(this.initial_backup);
				if(this.advance_backup!=null)
					this.node.setAdvanceDistribution(this.advance_backup);
			} catch(BNException e) {
				throw new CMException("Failed to restore parameters for node " + this.getName() + ": " + e.getMessage());
			}
		}
		
		protected Distribution initial_backup = null, advance_backup = null;
		protected IDBNNode node;
		private Vector<String> constColl = new Vector<String>();
	}
}

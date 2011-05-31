package complex.featural.controllers;

import bn.BNException;

import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.distributions.Distribution;
import bn.dynamic.ICountdownNode;
import bn.dynamic.IFDiscDBNNode;
import bn.messages.FiniteDiscreteMessage;

import complex.IParentProcess;
import complex.metrics.UsageProvider;

public class FHSMMX implements IParentProcess, UsageProvider
{
	FHSMMX(IFDiscDBNNode xnd, ICountdownNode znd, int ID)
	{
		this.xnd = xnd;
		this.znd = znd;
		this.ID = ID;
	}
	
	public String getName()
	{
		return this.xnd.getName();
	}
	
	public FiniteDiscreteMessage marginal(int t)
	{
		try {
			return this.xnd.getMarginal(t);
		} catch(BNException e) {
			System.err.println("Error : " + e.toString());
			return null;
		}
	}
	
	@Override
	public void backupParameters() {
		try {
		if(!this.xnd.isLocked())
		{
			this.qbackup = znd.getAdvanceDistribution().copy();
			this.Abackup = xnd.getAdvanceDistribution().copy();
			this.pibackup = xnd.getInitialDistribution().copy();
		}
		} catch(BNException e ) {
			System.err.println("Failed to backup parameters for hidden semi markov node.");
		}
	}
	@Override
	public void restoreParameters() {
		try {
		if(!this.xnd.isLocked() && this.Abackup != null && this.pibackup !=null && this.qbackup!=null)
		{
			this.xnd.setAdvanceDistribution(this.Abackup);
			this.xnd.setInitialDistribution(this.pibackup);
			this.znd.setAdvanceDistribution(this.qbackup);
		}
		} catch(BNException e) {
			System.err.println("Failed to restore parameters for node " + this.getName());
		}
	}
	
	public double[] usagets()
	{
		try {
			double[] usagets = new double[xnd.getNetwork().getT()];
			if(xnd.getCardinality()!=2)
				System.err.println("Error : Usage statistic only meant for binary nodes acting through or distributions.");
			for(int t = 0; t < xnd.getNetwork().getT(); t++)
				usagets[t] = xnd.getMarginal(t).getValue(1);
			return usagets;
		} catch(BNException e) {
			System.err.println("Error : failure to get usage.");
			return null;
		}
	}

	@Override
	public double totalusage() {
		try {
			double usage = 0;
			if(xnd.getCardinality()!=2)
				System.err.println("Error : Usage statistic only meant for binary nodes acting through or distributions.");
			for(int t = 0; t < xnd.getNetwork().getT(); t++)
				usage += xnd.getMarginal(t).getValue(1);
			return usage;
		} catch(BNException e) {
			System.err.println("Error : failure to get usage.");
			return Double.NaN;
		}
	}
	
	int ID;
	IFDiscDBNNode xnd;
	ICountdownNode znd;
	private Distribution qbackup;
	private DiscreteFiniteDistribution Abackup;
	private DiscreteFiniteDistribution pibackup;

}
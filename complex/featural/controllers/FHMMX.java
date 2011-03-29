package complex.featural.controllers;

import bn.BNException;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.dynamic.IFDiscDBNNode;
import bn.messages.FiniteDiscreteMessage;

import complex.IParentProcess;

public class FHMMX implements IParentProcess
{
	FHMMX(IFDiscDBNNode xnd,int ID)
	{
		this.xnd = xnd;
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
		if(!this.xnd.isLocked())
		{
			this.Abackup = xnd.getAdvanceDistribution();
			this.pibackup = xnd.getInitialDistribution();
		}
	}
	@Override
	public void restoreParameters() {
		try {
		if(!this.xnd.isLocked() && this.Abackup != null && this.pibackup !=null)
		{
			this.xnd.setAdvanceDistribution(this.Abackup);
			this.xnd.setInitialDistribution(this.pibackup);
		}
		} catch(BNException e) {
			System.err.println("Failed to restore parameters for node " + this.getName());
		}
	}
	
	int ID;
	IFDiscDBNNode xnd;
	private DiscreteFiniteDistribution Abackup;
	private DiscreteFiniteDistribution pibackup;
}
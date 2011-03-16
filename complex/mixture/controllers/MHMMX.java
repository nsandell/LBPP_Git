package complex.mixture.controllers;

import complex.IParentProcess;

import bn.BNException;
import bn.dynamic.IFDiscDBNNode;
import bn.messages.FiniteDiscreteMessage;

public class MHMMX implements IParentProcess
{
	MHMMX(IFDiscDBNNode xnd,int ID)
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
	int ID;
	IFDiscDBNNode xnd;
}

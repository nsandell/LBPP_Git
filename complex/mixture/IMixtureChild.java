package complex.mixture;

import bn.dynamic.IDBNNode;
import complex.IChildProcess;
import complex.IParentProcess;

public interface IMixtureChild extends IChildProcess {

	void setParent(IParentProcess rent);
	
	public static abstract class MixtureSingleNodeChild extends IChildProcess.SingleNodeChildProcess implements IMixtureChild
	{
		public MixtureSingleNodeChild(IDBNNode nd){super(nd);}
		
		@Override
		public void setParent(IParentProcess proc){}
	}
}

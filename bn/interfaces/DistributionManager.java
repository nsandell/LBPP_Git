package bn.interfaces;

import bn.distributions.Distribution;

public interface DistributionManager<DistributionType extends Distribution, ContextType>
{
	DistributionType getCPD(ContextType context);
	void setCPD(ContextType context, DistributionType dist);
}

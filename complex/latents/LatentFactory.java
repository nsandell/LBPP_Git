package complex.latents;

import bn.dynamic.IDynamicBayesNet;
import complex.CMException;
import complex.IParentProcess;

public interface LatentFactory {
	IParentProcess newLatent(String name, int id, IDynamicBayesNet net) throws CMException;
}

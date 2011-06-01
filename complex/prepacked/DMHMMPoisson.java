package complex.prepacked;

import java.util.HashMap;

import bn.BNException;

import complex.CMException;
import complex.prepacked.MHMM.IMixtureChildFactory;

public class DMHMMPoisson
{
	public static void main(String[] args) throws BNException, CMException
	{
		HashMap<String,IMixtureChildFactory> factories = new HashMap<String,IMixtureChildFactory>();
		factories.put("default",new MHMMPoisson.BPCFactory());
		factories.put("basic", new MHMMPoisson.BPCFactory());
		factories.put("ar", new MHMMPoisson.ARCFactory());
		factories.put("twit", new MHMMPoisson.TCFactory());
		DirichletMHMM.mhmm_main(args, factories);
	}
}

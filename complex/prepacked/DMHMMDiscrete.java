package complex.prepacked;

import java.util.HashMap;

import bn.BNException;

import complex.CMException;
import complex.prepacked.MHMM.IMixtureChildFactory;
import complex.prepacked.MHMMDiscrete.BDCFactory;

public class DMHMMDiscrete {
	public static void main(String[] args) throws BNException, CMException
	{
		HashMap<String,IMixtureChildFactory> factories = new HashMap<String,IMixtureChildFactory>();
		factories.put("default",new BDCFactory());
		factories.put("basic", new BDCFactory());
		DirichletMHMM.mhmm_main(args, factories);
	}
}

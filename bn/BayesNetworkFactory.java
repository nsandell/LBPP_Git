package bn;

import bn.interfaces.BayesNetI;

public class BayesNetworkFactory {
	public static BayesNetI getStaticNetwork(){return new BayesNet();}
}

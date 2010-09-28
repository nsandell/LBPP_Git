package bn.impl;

import bn.IDynBayesNet;
import bn.IStaticBayesNet;

public class BayesNetworkFactory {
	public static IStaticBayesNet getStaticNetwork(){return new StaticBayesianNetwork();}
	public static IDynBayesNet getDynamicNetwork(int T){return new DynamicBayesianNetwork(T);}
}

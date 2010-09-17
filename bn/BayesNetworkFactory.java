package bn;

import bn.interfaces.IDynBayesNet;
import bn.interfaces.IStaticBayesNet;

public class BayesNetworkFactory {
	public static IStaticBayesNet getStaticNetwork(){return new StaticBayesianNetwork();}
	public static IDynBayesNet getDynamicNetwork(int T){return new DynamicBayesianNetwork(T);}
}

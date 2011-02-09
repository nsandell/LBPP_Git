package bn.impl.dynbn;

import bn.dynamic.IDynNet;

public class DynamicNetworkFactory {
	private DynamicNetworkFactory(){}
	public static IDynNet newDynamicBayesNet(int T){return new DynamicBayesianNetwork(T);}
}

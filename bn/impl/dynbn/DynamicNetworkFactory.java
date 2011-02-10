package bn.impl.dynbn;

import bn.dynamic.IDynamicBayesNet;

public class DynamicNetworkFactory {
	private DynamicNetworkFactory(){}
	public static IDynamicBayesNet newDynamicBayesNet(int T){return new DynamicBayesianNetwork(T);}
}

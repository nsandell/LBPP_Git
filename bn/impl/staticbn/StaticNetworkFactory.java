package bn.impl.staticbn;

import bn.statc.IStaticBayesNet;

public class StaticNetworkFactory{
	private StaticNetworkFactory(){}
	public static IStaticBayesNet getNetwork(){return new StaticBayesianNetwork();}
}

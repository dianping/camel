package com.dianping.phoenix.lb.visitor;

import com.dianping.phoenix.lb.constant.Constants;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.service.VariableReplacer;
import com.dianping.phoenix.lb.utils.AdvEqualsBuilder;
import com.dianping.phoenix.lb.utils.PoolNameUtils;
import com.dianping.phoenix.lb.visitor.VirtualServerComparisionVisitor.ComparisionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Leo Liang
 *
 */
public class VirtualServerComparisionVisitor extends AbstractVisitor<ComparisionResult> {

	private static final String[] excludes = new String[] { "m_creationDate", "m_lastModifiedDate", "m_version",
			"m_instances" };
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private String vsName;
	private ModelWrapper oldVersion;
	private ModelWrapper newVersion;
	private boolean refChanged = false;

	public VirtualServerComparisionVisitor(String vsName, SlbModelTree slbModelTree) {

		result = new ComparisionResult();
		this.vsName = vsName;
		oldVersion = new ModelWrapper(slbModelTree.findVirtualServer(vsName), slbModelTree.getPools(),
				slbModelTree.getVariables(), slbModelTree.getAspects());
	}

	@Override
	public void visitAspect(Aspect aspect) {

		Aspect realAspect = aspect;
		Aspect oldAspect;

		if (!org.apache.commons.lang3.StringUtils.isEmpty(aspect.getRef())) {
			realAspect = newVersion.getAspect(aspect.getRef());
			oldAspect = oldVersion.getAspect(aspect.getRef());

			if (!AdvEqualsBuilder.reflectionEquals(realAspect, oldAspect, excludes)) {
				if (logger.isInfoEnabled()) {
					logger.info("[visitAspect][now vs old]" + realAspect + "[VS]" + oldAspect);
				}
				refChanged = true;
				return;
			}
		}

		for (Directive directive : realAspect.getDirectives()) {
			visitDirective(directive);
		}
	}

	@Override
	public void visitDirective(Directive directive) {
		if (refChanged) {
			return;
		}

		VariableReplacer newReplacer = new VariableReplacer(newVersion.getVariables());
		VariableReplacer oldReplacer = new VariableReplacer(oldVersion.getVariables());

		for (String value : directive.getDynamicAttributes().values()) {
			String newValue = newReplacer.translateValue(value);
			String oldValue = oldReplacer.translateValue(value);
			if (!oldValue.equals(newValue)) {
				if (logger.isInfoEnabled()) {
					logger.info("[visitDirective][" + value + "][now vs old]" + newValue + "[VS]" + oldValue);
				}
				refChanged = true;
				return;
			}
		}
	}

	public void visitSlbModelTree(SlbModelTree slbModelTree) {

		newVersion = new ModelWrapper(slbModelTree.findVirtualServer(vsName), slbModelTree.getPools(),
				slbModelTree.getVariables(), slbModelTree.getAspects());

		VirtualServer vs = slbModelTree.findVirtualServer(vsName);
		if (AdvEqualsBuilder.reflectionEquals(oldVersion.getVs(), vs, true, null, excludes)) {

			for (VirtualServer virtualServer : slbModelTree.getVirtualServers().values()) {
				visitVirtualServer(virtualServer);
			}

			if (refChanged) {
				if (logger.isInfoEnabled()) {
					logger.info("[visitSlbModelTree][refChanged]");
				}
				return;
			}
			Set<String> usedPoolNamePrefixs = getUsedPoolNamePrefixs(vs, slbModelTree.getPools());

			comparePools(usedPoolNamePrefixs, slbModelTree.getPools());
		} else {
			if (logger.isInfoEnabled()) {
				logger.info("[visitSlbModelTree][vs not equal]");
			}
		}
	}

	private Set<String> getUsedPoolNamePrefixs(VirtualServer vs, Map<String, Pool> pools) {

		Set<String> usedPoolNamePrefixs = new HashSet<String>();

		usedPoolNamePrefixs.add(vs.getDefaultPoolName());

		for (Location location : vs.getLocations()) {
			for (Directive directive : location.getDirectives()) {
				if (Constants.DIRECTIVE_PROXY_PASS.equals(directive.getType())) {
					usedPoolNamePrefixs.add(directive.getDynamicAttribute(Constants.DIRECTIVE_PROXY_PASS_POOL_NAME));
				} else {
					for (String value : directive.getDynamicAttributes().values()) {
						usedPoolNamePrefixs.add(PoolNameUtils.extractPoolNameFromProxyPassString(value));
					}
				}
			}
		}
		return usedPoolNamePrefixs;
	}

	private void comparePools(Set<String> usedPoolNamePrefixs, Map<String, Pool> pools) {
		for (Pool basePool : oldVersion.getPools().values()) {
			if (usedPoolNamePrefixs.contains(PoolNameUtils.getPoolNamePrefix(basePool.getName()))) {
				if (!pools.containsKey(basePool.getName())) {
					result.addDeletedPools(basePool);
				} else {
					if (!basePool.getLoadbalanceStrategyName()
							.equals(pools.get(basePool.getName()).getLoadbalanceStrategyName())) {
						return;
					}

					if (!AdvEqualsBuilder.reflectionEquals(basePool, pools.get(basePool.getName()), true, null,
							new String[] { "m_creationDate", "m_lastModifiedDate" })) {
						result.addModifiedPools(new PoolPair(basePool, pools.get(basePool.getName())));
					}
				}
			}
		}

		for (Pool pool : pools.values()) {
			if (usedPoolNamePrefixs.contains(PoolNameUtils.getPoolNamePrefix(pool.getName()))) {
				if (!oldVersion.getPools().containsKey(pool.getName())) {
					result.addAddedPools(pool);
				} else {
					if (!pool.getLoadbalanceStrategyName()
							.equals(oldVersion.getPools().get(pool.getName()).getLoadbalanceStrategyName())) {
						return;
					}

					PoolPair pair = new PoolPair(oldVersion.getPools().get(pool.getName()), pool);
					if (!result.getModifiedPools().contains(pair) && !AdvEqualsBuilder
							.reflectionEquals(pool, oldVersion.getPools().get(pool.getName()),
									new String[] { "m_creationDate", "m_lastModifiedDate" })) {
						result.addModifiedPools(pair);
					}
				}
			}
		}

		result.setNeedReload(false);
	}

	public static class PoolPair {

		private Pool oldPool;

		private Pool newPool;

		public PoolPair(Pool oldPool, Pool newPool) {
			this.oldPool = oldPool;
			this.newPool = newPool;
		}

		public Pool getOldPool() {
			return oldPool;
		}

		public void setOldPool(Pool oldPool) {
			this.oldPool = oldPool;
		}

		public Pool getNewPool() {
			return newPool;
		}

		public void setNewPool(Pool newPool) {
			this.newPool = newPool;
		}

		@Override
		public int hashCode() {

			return getTotalName().hashCode();
		}

		private String getTotalName() {

			return oldPool.getName() + "-" + newPool.getName();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof PoolPair)) {
				return false;
			}

			PoolPair poolPair = (PoolPair) obj;
			return getTotalName().equals(poolPair.getTotalName());
		}
	}

	public static class ComparisionResult {
		private boolean needReload = true;

		private List<Pool> addedPools = new ArrayList<Pool>();

		private List<Pool> deletedPools = new ArrayList<Pool>();

		private List<PoolPair> modifiedPools = new ArrayList<PoolPair>();

		public void setNeedReload(boolean needReload) {
			this.needReload = needReload;
		}

		public boolean needReload() {
			if (needReload) {
				return true;
			} else {
				if (containsConsistentPool(addedPools)) {
					return true;
				}
				if (containsConsistentPool(deletedPools)) {
					return true;
				}
				if (containsConsistentPoolInPair(modifiedPools)) {
					return true;
				}
				return false;
			}
		}

		private boolean containsConsistentPool(List<Pool> pools) {
			for (Pool pool : pools) {
				String loadbalanceName = pool.getLoadbalanceStrategyName();

				if (loadbalanceName != null && loadbalanceName.startsWith(Constants.LOAD_BALANCE_STRATEGY_CONSISTENT)) {
					return true;
				}
			}
			return false;
		}

		private boolean containsConsistentPoolInPair(List<PoolPair> poolPairs) {
			List<Pool> pools = new ArrayList<Pool>();

			for (PoolPair poolPair : poolPairs) {
				pools.add(poolPair.getNewPool());
				pools.add(poolPair.getOldPool());
			}
			return containsConsistentPool(pools);
		}

		public List<Pool> getAddedPools() {
			return addedPools;
		}

		public List<Pool> getDeletedPools() {
			return deletedPools;
		}

		public List<PoolPair> getModifiedPools() {
			return modifiedPools;
		}

		public void addAddedPools(Pool pool) {
			this.addedPools.add(pool);
		}

		public void addModifiedPools(PoolPair pool) {
			this.modifiedPools.add(pool);
		}

		public void addDeletedPools(Pool pool) {
			this.deletedPools.add(pool);
		}

	}

	private static class ModelWrapper {

		private VirtualServer vs;

		private Map<String, Pool> pools;

		private Map<String, String> variables;

		private Map<String, Aspect> aspects;

		public ModelWrapper(VirtualServer vs, Map<String, Pool> pools, List<Variable> lvariables,
				List<Aspect> laspects) {

			this.vs = vs;
			this.pools = pools;
			this.variables = new HashMap<String, String>();
			for (Variable variable : lvariables) {
				this.variables.put(variable.getKey(), variable.getValue());
			}
			this.aspects = new HashMap<String, Aspect>();
			for (Aspect aspect : laspects) {
				this.aspects.put(aspect.getName(), aspect);
			}
		}

		public Aspect getAspect(String ref) {
			return aspects.get(ref);
		}

		public Map<String, String> getVariables() {
			return variables;
		}

		public Map<String, Pool> getPools() {

			return pools;
		}

		public VirtualServer getVs() {
			return vs;
		}
	}

}

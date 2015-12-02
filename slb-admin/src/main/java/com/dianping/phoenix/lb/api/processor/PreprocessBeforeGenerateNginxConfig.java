package com.dianping.phoenix.lb.api.processor;

import com.dianping.phoenix.lb.model.entity.SlbModelTree;

/**
 * 在生成nginx配置文件之前，预处理slbmodel
 *
 * @author mengwenchao
 *         <p/>
 *         2014年12月10日 下午3:52:37
 */
public interface PreprocessBeforeGenerateNginxConfig {

	void process(SlbModelTree slbModelTree);
}

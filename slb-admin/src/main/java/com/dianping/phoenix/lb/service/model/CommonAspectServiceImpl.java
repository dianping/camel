/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at 2013-10-17
 */
package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.dao.CommonAspectDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Aspect;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Leo Liang
 *
 */
@Service
public class CommonAspectServiceImpl extends ConcurrentControlServiceTemplate implements CommonAspectService {
	private CommonAspectDao commonAspectDao;

	@Autowired(required = true)
	public CommonAspectServiceImpl(CommonAspectDao commonAspectDao) {
		super();
		this.commonAspectDao = commonAspectDao;
	}

	@Override
	public List<Aspect> listCommonAspects() {
		try {
			return read(new ReadOperation<List<Aspect>>() {

				@Override
				public List<Aspect> doRead() throws Exception {
					return commonAspectDao.list();
				}
			});
		} catch (BizException e) {
			// ignore
			return null;
		}
	}

	@Override
	public Aspect findCommonAspect(final String name) throws BizException {
		if (StringUtils.isBlank(name)) {
			ExceptionUtils.throwBizException(MessageID.COMMON_ASPECT_NAME_EMPTY);
		}

		return read(new ReadOperation<Aspect>() {

			@Override
			public Aspect doRead() throws BizException {
				return commonAspectDao.find(name);
			}
		});
	}

	@Override
	public void saveCommonAspect(final List<Aspect> aspects) throws BizException {
		if (aspects == null || aspects.isEmpty()) {
			return;
		}

		validate(aspects);

		write(new WriteOperation<Void>() {

			@Override
			public Void doWrite() throws Exception {
				commonAspectDao.save(aspects);
				return null;
			}
		});
	}

	private void validate(List<Aspect> aspects) throws BizException {
		for (Aspect aspect : aspects) {
			if (StringUtils.isBlank(aspect.getName())) {
				ExceptionUtils.throwBizException(MessageID.COMMON_ASPECT_NAME_EMPTY);
			}

			if (StringUtils.isNotBlank(aspect.getRef())) {
				ExceptionUtils.throwBizException(MessageID.COMMON_ASPECT_REF_NOT_EMPTY, aspect.getName());
			}

			if (aspect.getPointCut() == null) {
				ExceptionUtils.throwBizException(MessageID.COMMON_ASPECT_POINTCUT_NULL, aspect.getName());
			}
		}

	}
}

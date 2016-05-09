package com.dianping.phoenix.lb.service;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.utils.AbstractObservable;
import com.dianping.phoenix.lb.utils.ExceptionUtils;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * @author Leo Liang
 *
 */
public class ConcurrentControlServiceTemplate extends AbstractObservable {

	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private static ReadLock readLock = lock.readLock();

	private static WriteLock writeLock = lock.writeLock();

	public <T> T read(ReadOperation<T> readOp) throws BizException {
		readLock.lock();
		try {
			return readOp.doRead();
		} catch (Exception e) {
			ExceptionUtils.logAndRethrowBizException(e);
			// unreachable
			return null;
		} finally {
			readLock.unlock();
		}
	}

	public <T> T write(WriteOperation<T> writeOp) throws BizException {
		writeLock.lock();
		try {
			return writeOp.doWrite();
		} catch (Exception e) {
			ExceptionUtils.logAndRethrowBizException(e);
			// unreachable
			return null;
		} finally {
			writeLock.unlock();
		}
	}

	public interface ReadOperation<T> {
		T doRead() throws Exception;
	}

	public interface WriteOperation<T> {
		T doWrite() throws Exception;
	}

}

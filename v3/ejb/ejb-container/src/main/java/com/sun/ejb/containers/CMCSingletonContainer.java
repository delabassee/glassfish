/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.ejb.containers;

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.ejb.ComponentContext;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.InvocationInfo;
import com.sun.ejb.MethodLockInfo;

import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.LockType;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;

import org.glassfish.ejb.deployment.EjbSingletonDescriptor;

/**
 * @author Mahesh Kannan
 */
public class CMCSingletonContainer
        extends AbstractSingletonContainer {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();

    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    private final Lock defaultLock;

    private final MethodLockInfo defaultMethodLockInfo;

    public CMCSingletonContainer(EjbDescriptor desc, ClassLoader cl)
            throws Exception {
        super(desc, cl);
        LockType defaultLockType = ((EjbSingletonDescriptor) desc).getDefaultLockType();
        defaultLock = (defaultLockType == LockType.WRITE) ? writeLock : readLock;
        defaultMethodLockInfo = new MethodLockInfo(defaultLock == readLock,
                MethodLockInfo.NO_TIMEOUT, TimeUnit.SECONDS);
        System.out.println("** CMC Singleton Container **");
    }

    protected ComponentContext _getContext(EjbInvocation inv) {
        checkInit();
        InvocationInfo invInfo = inv.invocationInfo;

        MethodLockInfo lockInfo = (invInfo.methodLockInfo == null)
                ? defaultMethodLockInfo : invInfo.methodLockInfo;
        Lock theLock = lockInfo.isReadLockedMethod() ? readLock : writeLock;

        if (lockInfo.getTimeout() == MethodLockInfo.NO_TIMEOUT) {
            theLock.lock();
        } else {
            try {
                boolean lockStatus = theLock.tryLock(lockInfo.getTimeout(), lockInfo.getUnit());
                if (! lockStatus) {
                    String msg = "Couldn't acquire a lock within " + lockInfo.getTimeout() + " " + lockInfo.getUnit();
                    throw new ConcurrentAccessTimeoutException(msg);
                }
            } catch (InterruptedException inEx) {
                String msg = "Couldn't acquire a lock within " + lockInfo.getTimeout() + " " + lockInfo.getUnit();
                throw new ConcurrentAccessTimeoutException(msg);
            }
        }


        //Now that we have acquired the lock, remember it
        inv.setCMCLock(theLock);
        
        //Now that we have the lock return the singletonCtx
        return singletonCtx;
    }

    public void releaseContext(EjbInvocation inv) {
        Lock theLock = inv.getCMCLock();
        if (theLock != null) {
            theLock.unlock();
        }
    }

}

/*
 * Copyright (c) 2009-2014 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Simon McDuff - initial API and implementation
 *   Christian W. Damus (CEA LIST) - bug 399487
 */
package org.eclipse.emf.internal.cdo.transaction;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.commit.CDOCommitData;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.spi.common.commit.InternalCDOCommitInfoManager;
import org.eclipse.emf.cdo.util.CommitConflictException;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ContainmentCycleException;
import org.eclipse.emf.cdo.util.OptimisticLockingException;
import org.eclipse.emf.cdo.util.ReferentialIntegrityException;
import org.eclipse.emf.cdo.util.ValidationException;

import org.eclipse.net4j.util.om.monitor.EclipseMonitor;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import org.eclipse.emf.spi.cdo.CDOSessionProtocol;
import org.eclipse.emf.spi.cdo.CDOSessionProtocol.CommitTransactionResult;
import org.eclipse.emf.spi.cdo.CDOTransactionStrategy;
import org.eclipse.emf.spi.cdo.InternalCDOSavepoint;
import org.eclipse.emf.spi.cdo.InternalCDOSession;
import org.eclipse.emf.spi.cdo.InternalCDOTransaction;
import org.eclipse.emf.spi.cdo.InternalCDOTransaction.InternalCDOCommitContext;
import org.eclipse.emf.spi.cdo.InternalCDOUserSavepoint;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Simon McDuff
 * @since 2.0
 */
public class CDOSingleTransactionStrategyImpl implements CDOTransactionStrategy
{
  public static final CDOSingleTransactionStrategyImpl INSTANCE = new CDOSingleTransactionStrategyImpl();

  public CDOSingleTransactionStrategyImpl()
  {
  }

  public CDOCommitInfo commit(InternalCDOTransaction transaction, IProgressMonitor progressMonitor) throws Exception
  {
    InternalCDOCommitContext commitContext = transaction.createCommitContext();
    CDOCommitData commitData = commitContext.getCommitData();

    commitContext.preCommit();

    InternalCDOSession session = transaction.getSession();
    CDOSessionProtocol sessionProtocol = session.getSessionProtocol();

    OMMonitor monitor = progressMonitor != null ? new EclipseMonitor(progressMonitor) : null;
    CommitTransactionResult result = sessionProtocol.commitTransaction(commitContext, monitor);

    commitContext.postCommit(result);

    String rollbackMessage = result.getRollbackMessage();
    if (rollbackMessage != null)
    {
      byte rollbackReason = result.getRollbackReason();
      switch (rollbackReason)
      {
      case CDOProtocolConstants.ROLLBACK_REASON_OPTIMISTIC_LOCKING:
        throw new OptimisticLockingException(rollbackMessage);

      case CDOProtocolConstants.ROLLBACK_REASON_COMMIT_CONFLICT:
        throw new CommitConflictException(rollbackMessage);

      case CDOProtocolConstants.ROLLBACK_REASON_CONTAINMENT_CYCLE:
        throw new ContainmentCycleException(rollbackMessage);

      case CDOProtocolConstants.ROLLBACK_REASON_REFERENTIAL_INTEGRITY:
        throw new ReferentialIntegrityException(rollbackMessage, result.getXRefs());

      case CDOProtocolConstants.ROLLBACK_REASON_VALIDATION_ERROR:
        throw new ValidationException(rollbackMessage);

      case CDOProtocolConstants.ROLLBACK_REASON_UNKNOWN:
        throw new CommitException(rollbackMessage);

      default:
        throw new IllegalStateException("Invalid rollbackreason: " + rollbackReason);
      }
    }

    String comment = transaction.getCommitComment();
    transaction.setCommitComment(null);

    long previousTimeStamp = result.getPreviousTimeStamp();
    CDOBranch branch = transaction.getBranch();
    long timeStamp = result.getTimeStamp();
    String userID = session.getUserID();

    InternalCDOCommitInfoManager commitInfoManager = session.getCommitInfoManager();
    return commitInfoManager.createCommitInfo(branch, timeStamp, previousTimeStamp, userID, comment, commitData);
  }

  public void rollback(InternalCDOTransaction transaction, InternalCDOUserSavepoint savepoint)
  {
    transaction.handleRollback((InternalCDOSavepoint)savepoint);
  }

  public InternalCDOUserSavepoint setSavepoint(InternalCDOTransaction transaction)
  {
    return transaction.handleSetSavepoint();
  }

  public void setTarget(InternalCDOTransaction transaction)
  {
    // Do nothing
  }

  public void unsetTarget(InternalCDOTransaction transaction)
  {
    // Do nothing
  }
}

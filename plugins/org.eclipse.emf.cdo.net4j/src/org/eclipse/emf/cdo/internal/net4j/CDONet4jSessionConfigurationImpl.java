/***************************************************************************
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Andre Dietisheim - bug 256649
 **************************************************************************/
package org.eclipse.emf.cdo.internal.net4j;

import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfoManager;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.common.revision.CDORevisionManager;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.eresource.EresourcePackage;
import org.eclipse.emf.cdo.internal.common.model.CDOPackageRegistryImpl;
import org.eclipse.emf.cdo.internal.net4j.protocol.CDOClientProtocol;
import org.eclipse.emf.cdo.session.CDORepositoryInfo;
import org.eclipse.emf.cdo.spi.common.branch.CDOBranchUtil;
import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranchManager;
import org.eclipse.emf.cdo.spi.common.commit.CDOCommitInfoUtil;
import org.eclipse.emf.cdo.spi.common.commit.InternalCDOCommitInfoManager;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageRegistry;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;

import org.eclipse.emf.internal.cdo.session.CDOSessionConfigurationImpl;

import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.util.CheckUtil;
import org.eclipse.net4j.util.io.IStreamWrapper;

import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.spi.cdo.CDOSessionProtocol.OpenSessionResult;
import org.eclipse.emf.spi.cdo.CDOSessionProtocol.RepositoryTimeResult;
import org.eclipse.emf.spi.cdo.InternalCDOSession;

import java.util.Set;

/**
 * @author Eike Stepper
 */
public class CDONet4jSessionConfigurationImpl extends CDOSessionConfigurationImpl implements
    org.eclipse.emf.cdo.net4j.CDOSessionConfiguration
{
  private String repositoryName;

  private IConnector connector;

  private IStreamWrapper streamWrapper;

  private InternalCDOBranchManager branchManager;

  private InternalCDOPackageRegistry packageRegistry;

  private InternalCDORevisionManager revisionManager;

  private InternalCDOCommitInfoManager commitInfoManager;

  public CDONet4jSessionConfigurationImpl()
  {
  }

  public String getRepositoryName()
  {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName)
  {
    checkNotOpen();
    uncheckedSetRepositoryName(repositoryName);
  }

  protected void uncheckedSetRepositoryName(String repositoryName)
  {
    this.repositoryName = repositoryName;
  }

  public IConnector getConnector()
  {
    return connector;
  }

  public void setConnector(IConnector connector)
  {
    checkNotOpen();
    uncheckedSetConnector(connector);
  }

  protected void uncheckedSetConnector(IConnector connector)
  {
    this.connector = connector;
  }

  public IStreamWrapper getStreamWrapper()
  {
    return streamWrapper;
  }

  public void setStreamWrapper(IStreamWrapper streamWrapper)
  {
    checkNotOpen();
    this.streamWrapper = streamWrapper;
  }

  public InternalCDOBranchManager getBranchManager()
  {
    return branchManager;
  }

  public void setBranchManager(CDOBranchManager branchManager)
  {
    checkNotOpen();
    this.branchManager = (InternalCDOBranchManager)branchManager;
  }

  public InternalCDOPackageRegistry getPackageRegistry()
  {
    return packageRegistry;
  }

  public void setPackageRegistry(CDOPackageRegistry packageRegistry)
  {
    checkNotOpen();
    this.packageRegistry = (InternalCDOPackageRegistry)packageRegistry;
  }

  public InternalCDORevisionManager getRevisionManager()
  {
    return revisionManager;
  }

  public void setRevisionManager(CDORevisionManager revisionManager)
  {
    checkNotOpen();
    this.revisionManager = (InternalCDORevisionManager)revisionManager;
  }

  /**
   * Returns the commit info manager. The commit info manager may be used to query commit infos.
   * 
   * @return the commit info manager
   * @see CDOCommitInfoManager
   */
  public InternalCDOCommitInfoManager getCommitInfoManager()
  {
    return commitInfoManager;
  }

  /**
   * Sets the commit info manager. The commit info manager may be used to query commit infos. May only be called as long
   * as the session's not opened yet
   * 
   * @param commitInfoManager
   *          the new commit info manager
   * @see CDOCommitInfoManager
   */
  public void setCommitInfoManager(CDOCommitInfoManager commitInfoManager)
  {
    checkNotOpen();
    this.commitInfoManager = (InternalCDOCommitInfoManager)commitInfoManager;
  }

  @Override
  public org.eclipse.emf.cdo.net4j.CDOSession openSession()
  {
    return (org.eclipse.emf.cdo.net4j.CDOSession)super.openSession();
  }

  public InternalCDOSession createSession()
  {
    if (isActivateOnOpen())
    {
      CheckUtil.checkState(connector, "connector"); //$NON-NLS-1$
    }

    return new CDONet4jSessionImpl(this);
  }

  @Override
  public void activateSession(InternalCDOSession session) throws Exception
  {
    super.activateSession(session);
    OpenSessionResult result = initProtocol(session);

    if (packageRegistry == null)
    {
      packageRegistry = new CDOPackageRegistryImpl();
    }

    packageRegistry.setPackageProcessor(session);
    packageRegistry.setPackageLoader(session);
    packageRegistry.activate();

    if (revisionManager == null)
    {
      revisionManager = (InternalCDORevisionManager)CDORevisionUtil.createRevisionManager();
    }

    revisionManager.setSupportingBranches(session.getRepositoryInfo().isSupportingBranches());
    revisionManager.setRevisionLoader(session.getSessionProtocol());
    revisionManager.setRevisionLocker(session);
    revisionManager.activate();

    if (branchManager == null)
    {
      branchManager = CDOBranchUtil.createBranchManager();
    }

    branchManager.setBranchLoader(session.getSessionProtocol());
    branchManager.setTimeProvider(session.getRepositoryInfo());
    branchManager.initMainBranch(session.getRepositoryInfo().getCreationTime());
    branchManager.activate();

    if (commitInfoManager == null)
    {
      commitInfoManager = CDOCommitInfoUtil.createCommitInfoManager();
    }

    commitInfoManager.setCommitInfoLoader(session.getSessionProtocol());
    commitInfoManager.activate();

    for (InternalCDOPackageUnit packageUnit : result.getPackageUnits())
    {
      if (EcorePackage.eINSTANCE.getNsURI().equals(packageUnit.getID()))
      {
        EMFUtil.addAdapter(EcorePackage.eINSTANCE, packageUnit.getTopLevelPackageInfo());
        packageUnit.setState(CDOPackageUnit.State.LOADED);
      }
      else if (EresourcePackage.eINSTANCE.getNsURI().equals(packageUnit.getID()))
      {
        EMFUtil.addAdapter(EresourcePackage.eINSTANCE, packageUnit.getTopLevelPackageInfo());
        packageUnit.setState(CDOPackageUnit.State.LOADED);
      }

      getPackageRegistry().putPackageUnit(packageUnit);
    }
  }

  protected OpenSessionResult initProtocol(InternalCDOSession session)
  {
    CDOClientProtocol protocol = new CDOClientProtocol();
    protocol.setInfraStructure(session);
    if (streamWrapper != null)
    {
      protocol.setStreamWrapper(streamWrapper);
    }

    session.setSessionProtocol(protocol);
    protocol.open(connector);

    OpenSessionResult result = protocol.openSession(repositoryName, isPassiveUpdateEnabled(), getPassiveUpdateMode());
    session.setSessionID(result.getSessionID());
    session.setUserID(result.getUserID());
    session.setLastUpdateTime(result.getLastUpdateTime());
    session.setRepositoryInfo(new RepositoryInfo(repositoryName, result));
    return result;
  }

  @Override
  public void deactivateSession(InternalCDOSession session) throws Exception
  {
    commitInfoManager.deactivate();
    revisionManager.deactivate();

    // branchManager.deactivate();
    // packageRegistry.deactivate();

    super.deactivateSession(session);
  }

  /**
   * @author Eike Stepper
   */
  public class RepositoryInfo implements CDORepositoryInfo
  {
    private String name;

    private String uuid;

    private Type type;

    private State state;

    private String storeType;

    private Set<CDOID.ObjectType> objectIDTypes;

    private long creationTime;

    private RepositoryTimeResult timeResult;

    private CDOID rootResourceID;

    private boolean supportingAudits;

    private boolean supportingBranches;

    private boolean ensuringReferentialIntegrity;

    public RepositoryInfo(String name, OpenSessionResult result)
    {
      this.name = name;
      uuid = result.getRepositoryUUID();
      type = result.getRepositoryType();
      state = result.getRepositoryState();
      storeType = result.getStoreType();
      objectIDTypes = result.getObjectIDTypes();
      creationTime = result.getRepositoryCreationTime();
      timeResult = result.getRepositoryTimeResult();
      rootResourceID = result.getRootResourceID();
      supportingAudits = result.isRepositorySupportingAudits();
      supportingBranches = result.isRepositorySupportingBranches();
      ensuringReferentialIntegrity = result.isRepositoryEnsuringReferentialIntegrity();
    }

    public String getName()
    {
      return name;
    }

    /**
     * Must be callable before session activation has finished!
     */
    public String getUUID()
    {
      return uuid;
    }

    public Type getType()
    {
      return type;
    }

    public void setType(Type type)
    {
      this.type = type;
    }

    public State getState()
    {
      return state;
    }

    public void setState(State state)
    {
      this.state = state;
    }

    public String getStoreType()
    {
      return storeType;
    }

    public Set<CDOID.ObjectType> getObjectIDTypes()
    {
      return objectIDTypes;
    }

    public long getCreationTime()
    {
      return creationTime;
    }

    public long getTimeStamp()
    {
      return getTimeStamp(false);
    }

    public long getTimeStamp(boolean forceRefresh)
    {
      if (timeResult == null || forceRefresh)
      {
        timeResult = refreshTime();
      }

      return timeResult.getAproximateRepositoryTime();
    }

    public CDOID getRootResourceID()
    {
      return rootResourceID;
    }

    public boolean isSupportingAudits()
    {
      return supportingAudits;
    }

    public boolean isSupportingBranches()
    {
      return supportingBranches;
    }

    public boolean isEnsuringReferentialIntegrity()
    {
      return ensuringReferentialIntegrity;
    }

    private RepositoryTimeResult refreshTime()
    {
      return getSession().getSessionProtocol().getRepositoryTime();
    }
  }
}

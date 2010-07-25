/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Stefan Winkler - 271444: [DB] Multiple refactorings bug 271444
 *    Stefan Winkler - 249610: [DB] Support external references (Implementation)
 *
 */
package org.eclipse.emf.cdo.server.internal.db.mapping.horizontal;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionHandler;
import org.eclipse.emf.cdo.common.revision.CDORevisionManager;
import org.eclipse.emf.cdo.eresource.EresourcePackage;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStoreAccessor.QueryXRefsContext;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.server.db.IExternalReferenceManager;
import org.eclipse.emf.cdo.server.db.IMetaDataManager;
import org.eclipse.emf.cdo.server.db.IPreparedStatementCache;
import org.eclipse.emf.cdo.server.db.IPreparedStatementCache.ReuseProbability;
import org.eclipse.emf.cdo.server.db.mapping.IClassMapping;
import org.eclipse.emf.cdo.server.db.mapping.IListMapping;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.db.mapping.ITypeMapping;
import org.eclipse.emf.cdo.server.internal.db.CDODBSchema;
import org.eclipse.emf.cdo.server.internal.db.DBStore;
import org.eclipse.emf.cdo.server.internal.db.bundle.OM;
import org.eclipse.emf.cdo.spi.common.commit.CDOChangeSetSegment;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.db.DBType;
import org.eclipse.net4j.db.DBUtil;
import org.eclipse.net4j.db.ddl.IDBField;
import org.eclipse.net4j.db.ddl.IDBIndex;
import org.eclipse.net4j.db.ddl.IDBTable;
import org.eclipse.net4j.util.om.monitor.OMMonitor;
import org.eclipse.net4j.util.om.monitor.OMMonitor.Async;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMapUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 * @since 2.0
 */
public abstract class AbstractHorizontalClassMapping implements IClassMapping
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, AbstractHorizontalClassMapping.class);

  private EClass eClass;

  private IDBTable table;

  private AbstractHorizontalMappingStrategy mappingStrategy;

  private List<ITypeMapping> valueMappings;

  private List<IListMapping> listMappings;

  private Map<EStructuralFeature, String> unsettableFields;

  private String sqlSelectForHandle;

  private String sqlSelectForChangeSet;

  public AbstractHorizontalClassMapping(AbstractHorizontalMappingStrategy mappingStrategy, EClass eClass)
  {
    this.mappingStrategy = mappingStrategy;
    this.eClass = eClass;

    initTable();
    initFeatures();
    initSQLStrings();
  }

  private void initTable()
  {
    String name = getMappingStrategy().getTableName(eClass);
    table = getMappingStrategy().getStore().getDBSchema().addTable(name);

    IDBField idField = table.addField(CDODBSchema.ATTRIBUTES_ID, DBType.BIGINT, true);
    IDBField versionField = table.addField(CDODBSchema.ATTRIBUTES_VERSION, DBType.INTEGER, true);

    IDBField branchField = addBranchingField(table);

    table.addField(CDODBSchema.ATTRIBUTES_CLASS, DBType.BIGINT, true);
    table.addField(CDODBSchema.ATTRIBUTES_CREATED, DBType.BIGINT, true);
    IDBField revisedField = table.addField(CDODBSchema.ATTRIBUTES_REVISED, DBType.BIGINT, true);
    table.addField(CDODBSchema.ATTRIBUTES_RESOURCE, DBType.BIGINT, true);
    table.addField(CDODBSchema.ATTRIBUTES_CONTAINER, DBType.BIGINT, true);
    table.addField(CDODBSchema.ATTRIBUTES_FEATURE, DBType.INTEGER, true);

    if (branchField != null)
    {
      table.addIndex(IDBIndex.Type.UNIQUE, idField, versionField, branchField);
    }
    else
    {
      table.addIndex(IDBIndex.Type.UNIQUE, idField, versionField);
    }

    table.addIndex(IDBIndex.Type.NON_UNIQUE, idField, revisedField);
  }

  protected IDBField addBranchingField(IDBTable table)
  {
    return null;
  }

  private void initFeatures()
  {
    EStructuralFeature[] features = CDOModelUtil.getAllPersistentFeatures(eClass);

    if (features == null)
    {
      valueMappings = Collections.emptyList();
      listMappings = Collections.emptyList();
    }
    else
    {
      valueMappings = createValueMappings(features);
      listMappings = createListMappings(features);
    }
  }

  private void initSQLStrings()
  {
    // ----------- Select all revisions (for handleRevision) ---
    StringBuilder builder = new StringBuilder("SELECT "); //$NON-NLS-1$
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append(", "); //$NON-NLS-1$
    builder.append(CDODBSchema.ATTRIBUTES_VERSION);
    builder.append(" FROM "); //$NON-NLS-1$
    builder.append(getTable());
    sqlSelectForHandle = builder.toString();

    // ----------- Select all revisions (for handleRevision) ---
    builder = new StringBuilder("SELECT DISTINCT "); //$NON-NLS-1$
    builder.append(CDODBSchema.ATTRIBUTES_ID);
    builder.append(" FROM "); //$NON-NLS-1$
    builder.append(getTable());
    builder.append(" WHERE "); //$NON-NLS-1$
    sqlSelectForChangeSet = builder.toString();
  }

  private List<ITypeMapping> createValueMappings(EStructuralFeature[] features)
  {
    List<ITypeMapping> mappings = new ArrayList<ITypeMapping>();
    for (EStructuralFeature feature : features)
    {
      if (!feature.isMany())
      {
        ITypeMapping mapping = mappingStrategy.createValueMapping(feature);
        mapping.createDBField(getTable());
        mappings.add(mapping);

        if (feature.isUnsettable())
        {
          String fieldName = mappingStrategy.getUnsettableFieldName(feature);
          if (unsettableFields == null)
          {
            unsettableFields = new LinkedHashMap<EStructuralFeature, String>();
          }

          unsettableFields.put(feature, fieldName);
        }
      }
    }

    // add unsettable fields to end of table
    if (unsettableFields != null)
    {
      for (String fieldName : unsettableFields.values())
      {
        table.addField(fieldName, DBType.BOOLEAN, 1);
      }
    }

    return mappings;
  }

  private List<IListMapping> createListMappings(EStructuralFeature[] features)
  {
    List<IListMapping> listMappings = new ArrayList<IListMapping>();
    for (EStructuralFeature feature : features)
    {
      if (feature.isMany())
      {
        if (FeatureMapUtil.isFeatureMap(feature))
        {
          listMappings.add(mappingStrategy.createFeatureMapMapping(eClass, feature));
        }
        else
        {
          listMappings.add(mappingStrategy.createListMapping(eClass, feature));
        }
      }
    }

    return listMappings;
  }

  /**
   * Read the revision's values from the DB.
   * 
   * @return <code>true</code> if the revision has been read successfully.<br>
   *         <code>false</code> if the revision does not exist in the DB.
   */
  protected final boolean readValuesFromStatement(PreparedStatement pstmt, InternalCDORevision revision,
      IDBStoreAccessor accessor)
  {
    ResultSet resultSet = null;

    try
    {
      if (TRACER.isEnabled())
      {
        TRACER.format("Executing Query: {0}", pstmt.toString()); //$NON-NLS-1$
      }

      pstmt.setMaxRows(1); // Optimization: only 1 row

      resultSet = pstmt.executeQuery();
      if (!resultSet.next())
      {
        if (TRACER.isEnabled())
        {
          TRACER.format("Resultset was empty"); //$NON-NLS-1$
        }

        return false;
      }

      revision.setVersion(resultSet.getInt(CDODBSchema.ATTRIBUTES_VERSION));

      long timeStamp = resultSet.getLong(CDODBSchema.ATTRIBUTES_CREATED);

      CDOBranchPoint branchPoint = revision.getBranch().getPoint(timeStamp);

      revision.setBranchPoint(branchPoint);
      revision.setRevised(resultSet.getLong(CDODBSchema.ATTRIBUTES_REVISED));
      revision.setResourceID(CDODBUtil.convertLongToCDOID(getExternalReferenceManager(), accessor,
          resultSet.getLong(CDODBSchema.ATTRIBUTES_RESOURCE)));
      revision.setContainerID(CDODBUtil.convertLongToCDOID(getExternalReferenceManager(), accessor,
          resultSet.getLong(CDODBSchema.ATTRIBUTES_CONTAINER)));
      revision.setContainingFeatureID(resultSet.getInt(CDODBSchema.ATTRIBUTES_FEATURE));

      for (ITypeMapping mapping : valueMappings)
      {
        EStructuralFeature feature = mapping.getFeature();
        if (feature.isUnsettable())
        {
          if (!resultSet.getBoolean(unsettableFields.get(feature)))
          {
            // isSet==false -- setValue: null
            revision.setValue(feature, null);
            continue;
          }
        }

        mapping.readValueToRevision(resultSet, revision);
      }

      return true;
    }
    catch (SQLException ex)
    {
      throw new DBException(ex);
    }
    finally
    {
      DBUtil.close(resultSet);
    }
  }

  protected final void readLists(IDBStoreAccessor accessor, InternalCDORevision revision, int listChunk)
  {
    for (IListMapping listMapping : listMappings)
    {
      listMapping.readValues(accessor, revision, listChunk);
    }
  }

  protected final IMetaDataManager getMetaDataManager()
  {
    return getMappingStrategy().getStore().getMetaDataManager();
  }

  protected final IExternalReferenceManager getExternalReferenceManager()
  {
    return mappingStrategy.getStore().getExternalReferenceManager();
  }

  protected final IMappingStrategy getMappingStrategy()
  {
    return mappingStrategy;
  }

  public final EClass getEClass()
  {
    return eClass;
  }

  protected final Map<EStructuralFeature, String> getUnsettableFields()
  {
    return unsettableFields;
  }

  public final List<ITypeMapping> getValueMappings()
  {
    return valueMappings;
  }

  public final ITypeMapping getValueMapping(EStructuralFeature feature)
  {
    for (ITypeMapping mapping : valueMappings)
    {
      if (mapping.getFeature() == feature)
      {
        return mapping;
      }
    }

    return null;
  }

  public final List<IListMapping> getListMappings()
  {
    return listMappings;
  }

  public final IListMapping getListMapping(EStructuralFeature feature)
  {
    for (IListMapping mapping : listMappings)
    {
      if (mapping.getFeature() == feature)
      {
        return mapping;
      }
    }

    throw new IllegalArgumentException("List mapping for feature " + feature + " does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  protected final IDBTable getTable()
  {
    return table;
  }

  public List<IDBTable> getDBTables()
  {
    List<IDBTable> tables = new ArrayList<IDBTable>();
    tables.add(table);

    for (IListMapping listMapping : listMappings)
    {
      tables.addAll(listMapping.getDBTables());
    }

    return tables;
  }

  protected void checkDuplicateResources(IDBStoreAccessor accessor, CDORevision revision) throws IllegalStateException
  {
    CDOID folderID = (CDOID)revision.data().getContainerID();
    String name = (String)revision.data().get(EresourcePackage.eINSTANCE.getCDOResourceNode_Name(), 0);
    CDOID existingID = accessor.readResourceID(folderID, name, revision.getBranch().getHead());
    if (existingID != null && !existingID.equals(revision.getID()))
    {
      throw new IllegalStateException("Duplicate resource or folder: " + name + " in folder " + folderID); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  protected void writeLists(IDBStoreAccessor accessor, InternalCDORevision revision)
  {
    for (IListMapping listMapping : listMappings)
    {
      listMapping.writeValues(accessor, revision);
    }
  }

  public void writeRevision(IDBStoreAccessor accessor, InternalCDORevision revision, OMMonitor monitor)
  {
    Async async = null;
    monitor.begin(10);

    try
    {
      try
      {
        async = monitor.forkAsync();
        CDOID id = revision.getID();
        if (revision.getVersion() == CDORevision.FIRST_VERSION)
        {
          long timeStamp = revision.getTimeStamp();
          mappingStrategy.putObjectType(accessor, timeStamp, id, eClass);
        }
        else
        {
          long revised = revision.getTimeStamp() - 1;
          reviseOldRevision(accessor, id, revision.getBranch(), revised);
          for (IListMapping mapping : getListMappings())
          {
            mapping.objectDetached(accessor, id, revised);
          }
        }
      }
      finally
      {
        if (async != null)
        {
          async.stop();
        }
      }

      try
      {
        async = monitor.forkAsync();
        if (revision.isResourceFolder() || revision.isResource())
        {
          checkDuplicateResources(accessor, revision);
        }
      }
      finally
      {
        if (async != null)
        {
          async.stop();
        }
      }

      try
      {
        // Write attribute table always (even without modeled attributes!)
        async = monitor.forkAsync();
        writeValues(accessor, revision);
      }
      finally
      {
        if (async != null)
        {
          async.stop();
        }
      }

      try
      {
        // Write list tables only if they exist
        if (listMappings != null)
        {
          async = monitor.forkAsync(7);
          writeLists(accessor, revision);
        }
        else
        {
          monitor.worked(7);
        }
      }
      finally
      {
        if (async != null)
        {
          async.stop();
        }
      }
    }
    finally
    {
      monitor.done();
    }
  }

  public void handleRevisions(IDBStoreAccessor accessor, CDOBranch branch, long timeStamp, CDORevisionHandler handler)
  {
    // branch parameter is ignored, because either it is null or main branch.
    // this does not make any difference for non-branching store.
    // see #handleRevisions() implementation in HorizontalBranchingClassMapping
    // for branch handling.

    IPreparedStatementCache statementCache = accessor.getStatementCache();
    IRepository repository = accessor.getStore().getRepository();
    CDORevisionManager revisionManager = repository.getRevisionManager();
    CDOBranchManager branchManager = repository.getBranchManager();

    PreparedStatement stmt = null;
    ResultSet rs = null;

    // TODO: test for timeStamp == INVALID_TIME and encode revision.isValid() as WHERE instead of fetching all revisions
    // in order to increase performance

    StringBuilder builder = new StringBuilder(sqlSelectForHandle);

    if (timeStamp != CDOBranchPoint.UNSPECIFIED_DATE)
    {
      builder.append(" WHERE "); //$NON-NLS-1$
      builder.append(CDODBSchema.ATTRIBUTES_CREATED);
      builder.append("=? "); //$NON-NLS-1$
    }

    try
    {
      stmt = statementCache.getPreparedStatement(builder.toString(), ReuseProbability.LOW);
      if (timeStamp != CDOBranchPoint.UNSPECIFIED_DATE)
      {
        stmt.setLong(1, timeStamp);
      }

      rs = stmt.executeQuery();
      while (rs.next())
      {
        long id = rs.getLong(1);
        int version = rs.getInt(2);

        if (version >= CDOBranchVersion.FIRST_VERSION)
        {
          InternalCDORevision revision = (InternalCDORevision)revisionManager.getRevisionByVersion(
              CDOIDUtil.createLong(id), branchManager.getMainBranch().getVersion(version), CDORevision.UNCHUNKED, true);

          handler.handleRevision(revision);
        }
      }
    }
    catch (SQLException e)
    {
      throw new DBException(e);
    }
    finally
    {
      DBUtil.close(rs);
      statementCache.releasePreparedStatement(stmt);
    }
  }

  public Set<CDOID> readChangeSet(IDBStoreAccessor accessor, CDOChangeSetSegment[] segments)
  {
    StringBuilder builder = new StringBuilder(sqlSelectForChangeSet);
    boolean isFirst = true;

    for (int i = 0; i < segments.length; i++)
    {
      if (isFirst)
      {
        isFirst = false;
      }
      else
      {
        builder.append(" OR "); //$NON-NLS-1$
      }

      builder.append(CDODBSchema.ATTRIBUTES_CREATED);
      builder.append(">=?"); //$NON-NLS-1$
      builder.append(" AND ("); //$NON-NLS-1$
      builder.append(CDODBSchema.ATTRIBUTES_REVISED);
      builder.append("<=? OR "); //$NON-NLS-1$
      builder.append(CDODBSchema.ATTRIBUTES_REVISED);
      builder.append("="); //$NON-NLS-1$
      builder.append(DBStore.UNSPECIFIED_DATE);
      builder.append(")"); //$NON-NLS-1$
    }

    IPreparedStatementCache statementCache = accessor.getStatementCache();
    PreparedStatement stmt = null;
    ResultSet rs = null;

    Set<CDOID> result = new HashSet<CDOID>();

    try
    {
      stmt = statementCache.getPreparedStatement(builder.toString(), ReuseProbability.LOW);
      int col = 1;
      for (CDOChangeSetSegment segment : segments)
      {
        stmt.setLong(col++, segment.getTimeStamp());
        stmt.setLong(col++, segment.getEndTime());
      }

      rs = stmt.executeQuery();
      while (rs.next())
      {
        long id = rs.getLong(1);
        result.add(CDOIDUtil.createLong(id));
      }

      return result;
    }
    catch (SQLException e)
    {
      throw new DBException(e);
    }
    finally
    {
      DBUtil.close(rs);
      statementCache.releasePreparedStatement(stmt);
    }
  }

  public void detachObject(IDBStoreAccessor accessor, CDOID id, int version, CDOBranch branch, long timeStamp,
      OMMonitor monitor)
  {
    Async async = null;
    monitor.begin(1 + listMappings.size());

    try
    {
      if (version >= CDOBranchVersion.FIRST_VERSION)
      {
        reviseOldRevision(accessor, id, branch, timeStamp - 1);
      }

      detachAttributes(accessor, id, version + 1, branch, timeStamp, monitor.fork());

      // notify list mappings so they can clean up
      for (IListMapping mapping : getListMappings())
      {
        try
        {
          async = monitor.forkAsync();
          mapping.objectDetached(accessor, id, timeStamp);
        }
        finally
        {
          if (async != null)
          {
            async.stop();
          }
        }
      }
    }
    finally
    {
      monitor.done();
    }
  }

  public final boolean queryXRefs(IDBStoreAccessor accessor, QueryXRefsContext context, String idString)
  {
    String tableName = getTable().getName();
    EClass eClass = getEClass();
    List<EReference> refs = context.getSourceCandidates().get(eClass);
    List<EReference> scalarRefs = new ArrayList<EReference>();

    for (EReference ref : refs)
    {
      if (ref.isMany())
      {
        IListMapping listMapping = getListMapping(ref);
        String where = getListXRefsWhere(context);

        boolean more = listMapping.queryXRefs(accessor, tableName, where, context, idString);
        if (!more)
        {
          return false;
        }
      }
      else
      {
        scalarRefs.add(ref);
      }
    }

    if (!scalarRefs.isEmpty())
    {
      boolean more = queryScalarXRefs(accessor, scalarRefs, context, idString);
      if (!more)
      {
        return false;
      }
    }

    return true;
  }

  protected final boolean queryScalarXRefs(IDBStoreAccessor accessor, List<EReference> scalarRefs,
      QueryXRefsContext context, String idString)
  {
    String tableName = getTable().getName();
    String where = getListXRefsWhere(context);

    for (EReference ref : scalarRefs)
    {
      ITypeMapping valueMapping = getValueMapping(ref);
      String valueField = valueMapping.getField().getName();

      StringBuilder builder = new StringBuilder();
      builder.append("SELECT ");
      builder.append(CDODBSchema.ATTRIBUTES_ID);
      builder.append(", ");
      builder.append(valueField);
      builder.append(" FROM ");
      builder.append(tableName);
      builder.append(" WHERE ");
      builder.append(CDODBSchema.ATTRIBUTES_VERSION);
      builder.append(">0 AND ");
      builder.append(where);
      builder.append(" AND ");
      builder.append(valueField);
      builder.append(" IN ");
      builder.append(idString);
      String sql = builder.toString();

      ResultSet resultSet = null;
      Statement stmt = null;

      try
      {
        stmt = accessor.getConnection().createStatement();
        if (TRACER.isEnabled())
        {
          TRACER.format("Query XRefs (attributes): {0}", sql);
        }

        resultSet = stmt.executeQuery(sql);
        while (resultSet.next())
        {
          long idLong = resultSet.getLong(1);
          CDOID srcId = CDOIDUtil.createLong(idLong);
          idLong = resultSet.getLong(2);
          CDOID targetId = CDOIDUtil.createLong(idLong);

          boolean more = context.addXRef(targetId, srcId, ref, 0);
          if (TRACER.isEnabled())
          {
            TRACER.format("  add XRef to context: src={0}, tgt={1}, idx=0", srcId, targetId);
          }

          if (!more)
          {
            if (TRACER.isEnabled())
            {
              TRACER.format("  result limit reached. Ignoring further results.");
            }

            return false;
          }
        }
      }
      catch (SQLException ex)
      {
        throw new DBException(ex);
      }
      finally
      {
        DBUtil.close(resultSet);
        DBUtil.close(stmt);
      }
    }

    return true;
  }

  protected abstract String getListXRefsWhere(QueryXRefsContext context);

  protected abstract void detachAttributes(IDBStoreAccessor accessor, CDOID id, int version, CDOBranch branch,
      long timeStamp, OMMonitor fork);

  protected abstract void reviseOldRevision(IDBStoreAccessor accessor, CDOID id, CDOBranch branch, long timeStamp);

  protected abstract void writeValues(IDBStoreAccessor accessor, InternalCDORevision revision);
}

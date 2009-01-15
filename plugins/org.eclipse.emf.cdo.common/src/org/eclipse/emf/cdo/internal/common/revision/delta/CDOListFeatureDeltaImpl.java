/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 */
package org.eclipse.emf.cdo.internal.common.revision.delta;

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.model.CDOClass;
import org.eclipse.emf.cdo.common.model.CDOFeature;
import org.eclipse.emf.cdo.common.revision.CDOReferenceAdjuster;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDeltaVisitor;
import org.eclipse.emf.cdo.common.revision.delta.CDOListFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDORemoveFeatureDelta;

import org.eclipse.net4j.util.collection.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Simon McDuff
 */
public class CDOListFeatureDeltaImpl extends CDOFeatureDeltaImpl implements CDOListFeatureDelta
{
  private List<CDOFeatureDelta> featureDeltas = new ArrayList<CDOFeatureDelta>();

  transient private int[] cacheIndices = null;

  transient private IListTargetAdding[] cacheSources = null;

  transient private List<CDOFeatureDelta> notProcessedFeatureDelta = null;

  public CDOListFeatureDeltaImpl(CDOFeature feature)
  {
    super(feature);
  }

  public CDOListFeatureDeltaImpl(CDODataInput in, CDOClass cdoClass) throws IOException
  {
    super(in, cdoClass);
    int size = in.readInt();
    for (int i = 0; i < size; i++)
    {
      featureDeltas.add(in.readCDOFeatureDelta(cdoClass));
    }
  }

  @Override
  public CDOListFeatureDelta copy()
  {
    CDOListFeatureDeltaImpl list = new CDOListFeatureDeltaImpl(getFeature());
    for (CDOFeatureDelta delta : featureDeltas)
    {
      list.add(((InternalCDOFeatureDelta)delta).copy());
    }

    return list;
  }

  @Override
  public void write(CDODataOutput out, CDOClass cdoClass) throws IOException
  {
    super.write(out, cdoClass);
    out.writeInt(featureDeltas.size());
    for (CDOFeatureDelta featureDelta : featureDeltas)
    {
      out.writeCDOFeatureDelta(featureDelta, cdoClass);
    }
  }

  public Type getType()
  {
    return Type.LIST;
  }

  public List<CDOFeatureDelta> getListChanges()
  {
    return featureDeltas;
  }

  /**
   * Returns the number of indices as the first element of the array.
   * 
   * @return never <code>null</code>.
   */

  public Pair<IListTargetAdding[], int[]> reconstructAddedIndices()
  {
    reconstructAddedIndicesWithNoCopy();
    return new Pair<IListTargetAdding[], int[]>(Arrays.copyOf(cacheSources, cacheSources.length), Arrays.copyOf(
        cacheIndices, cacheIndices.length));
  }

  private void reconstructAddedIndicesWithNoCopy()
  {
    if (cacheIndices == null || notProcessedFeatureDelta != null)
    {
      if (cacheIndices == null)
      {
        cacheIndices = new int[1 + featureDeltas.size()];
      }
      else if (cacheIndices.length <= 1 + featureDeltas.size())
      {
        int newCapacity = Math.max(10, cacheIndices.length * 3 / 2 + 1);
        int[] newElements = new int[newCapacity];
        System.arraycopy(cacheIndices, 0, newElements, 0, cacheIndices.length);
        cacheIndices = newElements;
      }

      if (cacheSources == null)
      {
        cacheSources = new IListTargetAdding[1 + featureDeltas.size()];
      }
      else if (cacheSources.length <= 1 + featureDeltas.size())
      {
        int newCapacity = Math.max(10, cacheSources.length * 3 / 2 + 1);
        IListTargetAdding[] newElements = new IListTargetAdding[newCapacity];
        System.arraycopy(cacheSources, 0, newElements, 0, cacheSources.length);
        cacheSources = newElements;
      }
      List<CDOFeatureDelta> featureDeltasToBeProcess = notProcessedFeatureDelta == null ? featureDeltas
          : notProcessedFeatureDelta;

      for (CDOFeatureDelta featureDelta : featureDeltasToBeProcess)
      {
        if (featureDelta instanceof IListIndexAffecting)
        {
          IListIndexAffecting affecting = (IListIndexAffecting)featureDelta;
          affecting.affectIndices(cacheSources, cacheIndices);
        }

        if (featureDelta instanceof IListTargetAdding)
        {
          cacheIndices[++cacheIndices[0]] = ((IListTargetAdding)featureDelta).getIndex();
          cacheSources[cacheIndices[0]] = (IListTargetAdding)featureDelta;
        }
      }
      notProcessedFeatureDelta = null;
    }
  }

  private void cleanupWithNewDelta(CDOFeatureDelta featureDelta)
  {
    CDOFeature feature = getFeature();
    if (feature.isReference() && featureDelta instanceof CDORemoveFeatureDelta)
    {
      int indexToRemove = ((CDORemoveFeatureDelta)featureDelta).getIndex();

      reconstructAddedIndicesWithNoCopy();

      for (int i = 1; i <= cacheIndices[0]; i++)
      {
        int index = cacheIndices[i];
        if (indexToRemove == index)
        {
          cacheSources[i].clear();
          break;
        }
      }
    }

    if (cacheIndices != null)
    {
      if (notProcessedFeatureDelta == null)
      {
        notProcessedFeatureDelta = new ArrayList<CDOFeatureDelta>();
      }
      notProcessedFeatureDelta.add(featureDelta);
    }
  }

  public void add(CDOFeatureDelta featureDelta)
  {
    cleanupWithNewDelta(featureDelta);
    featureDeltas.add(featureDelta);
  }

  public void apply(CDORevision revision)
  {
    for (CDOFeatureDelta featureDelta : featureDeltas)
    {
      ((CDOFeatureDeltaImpl)featureDelta).apply(revision);
    }
  }

  @Override
  public void adjustReferences(CDOReferenceAdjuster adjuster)
  {
    for (CDOFeatureDelta featureDelta : featureDeltas)
    {
      ((CDOFeatureDeltaImpl)featureDelta).adjustReferences(adjuster);
    }
  }

  public void accept(CDOFeatureDeltaVisitor visitor)
  {
    visitor.visit(this);
  }
}

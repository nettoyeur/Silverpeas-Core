/*
 * Copyright (C) 2000 - 2011 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection withWriter Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.silverpeas.pdc.service;

import java.util.List;
import com.silverpeas.SilverpeasContent;
import com.silverpeas.pdc.dao.PdcClassificationDAO;
import com.silverpeas.pdc.model.PdcClassification;
import com.stratelia.silverpeas.pdc.control.PdcBm;
import com.stratelia.silverpeas.pdc.model.ClassifyPosition;
import com.stratelia.silverpeas.pdc.model.PdcException;
import com.stratelia.silverpeas.pdc.model.PdcRuntimeException;
import com.stratelia.webactiv.util.EJBUtilitaire;
import com.stratelia.webactiv.util.JNDINames;
import com.stratelia.webactiv.util.exception.SilverpeasRuntimeException;
import com.stratelia.webactiv.util.node.control.NodeBm;
import com.stratelia.webactiv.util.node.control.NodeBmHome;
import com.stratelia.webactiv.util.node.model.NodeDetail;
import com.stratelia.webactiv.util.node.model.NodePK;
import java.rmi.RemoteException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import static com.silverpeas.pdc.model.PdcClassification.*;

/**
 * The service aiming at classifying the contents in Silverpeas on the classification plan (named
 * PdC).
 * 
 * The classification of a content in Silverpeas consists to position it on the PdC; it is the
 * process to attribute some semantic metadata to the content. The metadata then can be used to find
 * any contents that satisfies a search of information by keywords. As some contents can not to be 
 * positioned on the PdC (it is not mandatory), they can be not detected in the search.
 * 
 * The classification of a content on the PdC can be done in one of the two ways:
 * <ul>
 * <li>the contributor can position explicitly the content on the PdC,</li>
 * <li>a predefined classification is used either to classify automatically the content or as a
 * template to classify the content.</li>
 * </ul>
 * 
 * A predefined classification on the PdC can be created for a given node in a Silverpeas component
 * instance or for the whole component instance. The predefined classification is a way to facilitate
 * or to impose the classification of the contents on the PdC. A node in Silverpeas is a way to
 * hierarchically categorize a content. A node can represents for example a topic. A node is part of
 * a hierarchic tree and can then contain both some contents and some nodes. So, a predefined
 * classification on the PdC that is associated with a node, is set for all the contents in its
 * children nodes. Therefore, if a predefined classification is not found for a given node, then
 * it is seeked back upto the root node (that is the component instance ifself).
 */
@Named
@Transactional
public class PdcClassificationService {

  @Inject
  private PdcClassificationDAO dao;
  private NodeBm nodeBm;
  @Inject
  private PdcBm pdcBm;

  /**
   * Gets the predefined classification on the PdC that was set for any new contents in the specified
   * node of the specified component instance.
   * 
   * If the predefined classification is not found for the specified node, then it is seeked back upto
   * the root node (that is the component instance ifself). In the case no predefined classification
   * isn't set for the whole component instance, an empty classification is then returned.
   * @param nodeId the unique identifier of the node.
   * @param instanceId the unique identifier of the Silverpeas component instance.
   * @return a predefined classification on the PdC ready to be used to classify a content published
   * in the specified node or an empty classification.
   */
  public PdcClassification getPreDefinedClassification(String nodeId, String instanceId) {
    try {
      PdcClassification classification = null;
      NodePK nodeToSeek = new NodePK(nodeId, instanceId);
      while (classification == null && !nodeToSeek.isUndefined()) {
        classification = dao.findPredefinedClassificationByNodeId(nodeToSeek.getId(),
                nodeToSeek.getInstanceId());
        NodeDetail node = getNodeBm().getDetail(nodeToSeek);
        nodeToSeek = node.getFatherPK();
      }
      if (classification == null) {
        classification = getPreDefinedClassification(instanceId);
      }
      return classification;
    } catch (RemoteException ex) {
      throw new PdcRuntimeException(getClass().getSimpleName() + ".getPreDefinedClassification()",
              SilverpeasRuntimeException.ERROR, "root.EX_CANT_GET_REMOTE_OBJECT",
              ex);
    } catch (Exception ex) {
      throw new EntityNotFoundException(ex.getMessage());
    }
  }

  /**
   * Gets the predefined classification on the PdC that was set for any new contents in the specified
   * managed by the specified component instance. This method is for the component instances that
   * don't support the categorization.
   * 
   * In the case no predefined classification isn't set for the whole component instance, a none
   * classification is then returned.
   * @param instanceId the unique identifier of the Silverpeas component instance.
   * @return a predefined classification on the PdC ready to be used to classify a content
   * published within the component instance or an empty classification.
   */
  public PdcClassification getPreDefinedClassification(String instanceId) {
    PdcClassification classification = dao.findPredefinedClassificationByComponentInstanceId(
            instanceId);
    if (classification == null) {
      classification = NONE_CLASSIFICATION;
    }
    return classification;
  }

  /**
   * Saves the specified predefined classification on the PdC. If a predefined classification
   * already exists for the node (if any) and the component instance to which this classification is
   * related, then it is replaced by the specified one.
   * 
   * The node (if any) and the component instance for which this classification has to be saved
   * are indicated by the specified classification itself. If no node is refered by it, then the
   * predefined classification will serv for the whole component instance.
   * @param classification the predefined classification to register.
   */
  public PdcClassification savePreDefinedClassification(final PdcClassification classification) {
    if (!classification.isPredefined()) {
      throw new IllegalArgumentException("The classification isn't a predefined one");
    }
    PdcClassification savedClassification = dao.saveAndFlush(classification);
    return savedClassification;
  }

  /**
   * Classifies the specified content on the PdC with the specified classification.
   * The content must exist in Silverpeas before being classified.
   * If an error occurs while classifying the content, a runtime exception PdcRuntimeException is
   * thrown.
   * @param content the Silverpeas content to classify.
   * @param withClassification the classification with which the content is positioned on the PdC.
   */
  public void classifyContent(final SilverpeasContent content,
          final PdcClassification withClassification) throws PdcRuntimeException {
    List<ClassifyPosition> positions = withClassification.getClassifyPositions();
    try {
      for (ClassifyPosition classifyPosition : positions) {
        int silverObjectId = Integer.valueOf(content.getSilverpeasContentId());
        pdcBm.addPosition(silverObjectId, classifyPosition, content.getComponentInstanceId());
      }
    } catch (PdcException ex) {
      throw new PdcRuntimeException(getClass().getSimpleName() + ".classifyContent()", ex.
              getErrorLevel(), ex.getMessage(), ex);
    }
  }

  /**
   * A convenient method to enhance the readability of method calls.
   * @param classification a classification on the PdC.
   * @return the classification.
   */
  public static PdcClassification withClassification(final PdcClassification classification) {
    return classification;
  }

  protected NodeBm getNodeBm() {
    if (nodeBm == null) {
      try {
        NodeBmHome home = (NodeBmHome) EJBUtilitaire.getEJBObjectRef(
                JNDINames.NODEBM_EJBHOME, NodeBmHome.class);
        nodeBm = home.create();
      } catch (Exception ex) {
        throw new PdcRuntimeException(getClass().getSimpleName() + ".getNodeBm()",
                SilverpeasRuntimeException.ERROR, "root.EX_CANT_GET_REMOTE_OBJECT",
                ex);
      }
    }
    return nodeBm;
  }

  protected void setNodeBm(final NodeBm nodeBm) {
    this.nodeBm = nodeBm;
  }
}
/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.silverpeas.comment.dao.jdbc;

import com.silverpeas.comment.CommentRuntimeException;
import com.silverpeas.comment.dao.CommentDAO;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import com.silverpeas.util.ForeignPK;
import com.silverpeas.comment.model.Comment;
import com.silverpeas.comment.dao.CommentInfo;
import com.silverpeas.comment.dao.CommentInfoComparator;
import com.silverpeas.comment.model.CommentPK;
import com.stratelia.silverpeas.silvertrace.SilverTrace;
import com.stratelia.webactiv.beans.admin.OrganizationController;
import com.stratelia.webactiv.beans.admin.UserDetail;
import com.stratelia.webactiv.util.DBUtil;
import com.stratelia.webactiv.util.JNDINames;
import com.stratelia.webactiv.util.exception.SilverpeasRuntimeException;
import org.springframework.stereotype.Repository;

@Repository("commentDAO")
public class JDBCCommentDAO implements CommentDAO {

  private static final long serialVersionUID = -4880326368611108874L;

  private Connection openConnection() {
    try {
      Connection con = DBUtil.makeConnection(JNDINames.NODE_DATASOURCE);
      return con;
    } catch (Exception e) {
      throw new CommentRuntimeException(getClass().getSimpleName() + ".getConnection()",
          SilverpeasRuntimeException.ERROR, "root.EX_CONNECTION_OPEN_FAILED", e);
    }
  }

  private void closeConnection(Connection con) {
    if (con != null) {
      try {
        con.close();
      } catch (Exception e) {
        SilverTrace.error("comment", getClass().getSimpleName() + ".closeConnection()",
            "root.EX_CONNECTION_CLOSE_FAILED", "", e);
      }
    }
  }

  private JDBCCommentRequester getCommentDAO() {
    return new JDBCCommentRequester();
  }

  @Override
  public CommentPK saveComment(Comment cmt) {
    Connection con = openConnection();
    CommentPK commentPK;
    try {
      JDBCCommentRequester commentDAO = getCommentDAO();
      commentPK = commentDAO.saveComment(con, cmt);
      if (commentPK == null) {
        throw new CommentRuntimeException(getClass().getSimpleName() + ".createComment()",
            SilverpeasRuntimeException.ERROR,
            "comment.CREATING_NEW_COMMENT_FAILED");
      }
      return commentPK;
    } catch (Exception re) {
      throw new CommentRuntimeException(getClass().getSimpleName() + ".createComment()",
          SilverpeasRuntimeException.ERROR,
          "comment.CREATING_NEW_COMMENT_FAILED", re);
    } finally {
      closeConnection(con);
    }
  }

  @Override
  public void removeComment(CommentPK pk) {
    Connection con = openConnection();
    try {
      JDBCCommentRequester commentDAO = getCommentDAO();
      commentDAO.deleteComment(con, pk);
    } catch (Exception re) {
      throw new CommentRuntimeException(getClass().getSimpleName() + ".deleteComment()",
          SilverpeasRuntimeException.ERROR, "comment.DELETE_COMMENT_FAILED", re);
    } finally {
      closeConnection(con);
    }
  }

  @Override
  public void updateComment(Comment cmt) {
    Connection con = openConnection();
    try {
      JDBCCommentRequester commentDAO = getCommentDAO();
      commentDAO.updateComment(con, cmt);
    } catch (Exception re) {
      throw new CommentRuntimeException(getClass().getSimpleName() + ".updateComment()",
          SilverpeasRuntimeException.ERROR, "comment.UPDATE_COMMENT_FAILED", re);
    } finally {
      closeConnection(con);
    }
  }

  @Override
  public Comment getComment(CommentPK pk)  {
    Connection con = openConnection();
    Comment comment;
    try {
      JDBCCommentRequester commentDAO = getCommentDAO();
      comment = commentDAO.getComment(con, pk);
      if (comment == null) {
        throw new CommentRuntimeException(getClass().getSimpleName() + ".getComment()",
            SilverpeasRuntimeException.ERROR, "comment.GET_COMMENT_FAILED");
      }
      return comment;
    } catch (Exception re) {
      throw new CommentRuntimeException(getClass().getSimpleName() + ".getComment()",
          SilverpeasRuntimeException.ERROR, "comment.GET_COMMENT_FAILED", re);
    } finally {
      closeConnection(con);
    }
  }

  @Override
  public List<CommentInfo> getAllMostCommentedPublications() {
    Connection con = openConnection();
    try {
      JDBCCommentRequester commentDAO = getCommentDAO();
      return commentDAO.getMostCommentedAllPublications(con);
    } catch (Exception e) {
      throw new CommentRuntimeException(getClass().getSimpleName() + ".getMostCommentedAllPublications()",
          SilverpeasRuntimeException.FATAL,
          "comment.GET_COMMENT_FAILED",
          e);
    } finally {
      closeConnection(con);
    }
  }

  @Override
  public List<CommentInfo> getMostCommentedPublications(final Collection<ForeignPK> pks, int commentsCount) {
    List<CommentInfo> comments = new ArrayList<CommentInfo>();
    Connection con = openConnection();
    JDBCCommentRequester commentDAO = getCommentDAO();
    if (pks != null && !pks.isEmpty()) {
      try {
        for (ForeignPK foreignPk : pks) {
          comments.add(new CommentInfo(commentDAO.getCommentsCount(con,
              foreignPk), foreignPk.getInstanceId(), foreignPk.getId()));
        }
        Collections.sort(comments, new CommentInfoComparator());
        if (comments.size() > commentsCount) {
          comments.subList(0, commentsCount);
        }
      } catch (Exception e) {

      } finally {
        closeConnection(con);
      }
    }
    return comments;
  }

  @Override
  public int getCommentsCountByForeignKey(ForeignPK foreign_pk) {
    Connection con = openConnection();
    int publicationCommentsCount = 0;
    try {
      JDBCCommentRequester commentDAO = getCommentDAO();
      publicationCommentsCount = commentDAO.getCommentsCount(con, foreign_pk);
    } catch (Exception re) {
      throw new CommentRuntimeException(getClass().getSimpleName() + ".getCommentsCount()",
          SilverpeasRuntimeException.ERROR, "comment.GET_ALL_COMMENTS_FAILED",
          re);
    } finally {
      closeConnection(con);
    }
    return publicationCommentsCount;
  }

  @Override
  public List<Comment> getAllCommentsByForeignKey(ForeignPK foreign_pk) {
    Connection con = openConnection();
    List<Comment> vRet;
    try {
      JDBCCommentRequester commentDAO = getCommentDAO();
      vRet = commentDAO.getAllComments(con, foreign_pk);
      if (vRet == null) {
        throw new CommentRuntimeException(getClass().getSimpleName() + ".getAllComments()",
            SilverpeasRuntimeException.ERROR, "comment.GET_ALL_COMMENTS_FAILED");
      }
      return vRet;
    } catch (Exception re) {
      throw new CommentRuntimeException(getClass().getSimpleName() + ".getAllComments()",
          SilverpeasRuntimeException.ERROR, "comment.GET_ALL_COMMENTS_FAILED",
          re);
    } finally {
      closeConnection(con);
    }
  }

  private static String getUserName(UserDetail userDetail) {
    return userDetail.getFirstName() + " " + userDetail.getLastName();
  }

  private static String getUserName(Comment cmt) {
    UserDetail userDetail = (new OrganizationController()).getUserDetail(String
        .valueOf(cmt.getOwnerId()));
    return getUserName(userDetail);
  }

  @Override
  public void removeAllCommentsByForeignPk(ForeignPK foreign_pk) {
    Connection con = openConnection();
    try {
      JDBCCommentRequester commentDAO = getCommentDAO();
      commentDAO.deleteAllComments(con, foreign_pk);
    } catch (Exception re) {
      throw new CommentRuntimeException(getClass().getSimpleName() + ".getAllComments()",
          SilverpeasRuntimeException.ERROR, "comment.GET_ALL_COMMENTS_FAILED",
          re);
    } finally {
      closeConnection(con);
    }
  }

  @Override
  public void moveComments(ForeignPK fromPK, ForeignPK toPK) {
    Connection con = openConnection();
    try {
      JDBCCommentRequester commentDAO = getCommentDAO();
      commentDAO.moveComments(con, fromPK, toPK);
    } catch (Exception re) {
      throw new CommentRuntimeException(getClass().getSimpleName() + ".moveComments()",
          SilverpeasRuntimeException.ERROR, "comment.GET_ALL_COMMENTS_FAILED",
          re);
    } finally {
      closeConnection(con);
    }
  }
}
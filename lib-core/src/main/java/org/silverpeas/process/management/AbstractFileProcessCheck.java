/*
 * Copyright (C) 2000 - 2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.process.management;

import org.silverpeas.process.check.AbstractProcessCheck;
import org.silverpeas.process.check.ProcessCheckType;
import org.silverpeas.process.io.file.FileHandler;

/**
 * Abstract extension of <code>AbstractProcessCheck</code> oriented on file system verifications.
 * @author Yohann Chastagnier
 * @see AbstractProcessCheck
 */
public abstract class AbstractFileProcessCheck extends AbstractProcessCheck {

  /*
   * (non-Javadoc)
   * @see org.silverpeas.process.check.Check#getType()
   */
  @Override
  public ProcessCheckType getType() {
    return ProcessCheckType.FILESYSTEM;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.silverpeas.process.check.Check#check(org.silverpeas.process.management.ProcessExecutionContext
   * )
   */
  @Override
  public final void check(final ProcessExecutionContext processExecutionProcess) throws Exception {
    checkFiles(processExecutionProcess, processExecutionProcess.getFileHandler());
  }

  /**
   * Contains the treatment of the verification. The file handler (@see {@link FileHandler})
   * associated to the current execution of chained Silverpeas processes is passed.
   * @param processExecutionProcess
   * @param fileHandler
   * @throws Exception
   */
  abstract public void checkFiles(ProcessExecutionContext processExecutionProcess,
      final FileHandler fileHandler) throws Exception;
}

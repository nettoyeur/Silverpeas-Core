/*
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

package com.stratelia.silverpeas.scheduler.trigger;

import com.stratelia.silverpeas.scheduler.TimeUnit;

/**
 * A job trigger that fires repeatedly the execution of a job at a specified interval.
 */
public final class FixedPeriodJobTrigger extends JobTrigger {
  
  private int time;
  private TimeUnit unit;

  /**
   * Gets the interval in time between two job triggering.
   * @return the time interval.
   */
  public int getTimeInterval() {
    return time;
  }
  
  /**
   * Creates a new job trigger that will fire a job execution at a specified interval.
   * @param time the interval in the time between each job triggering.
   * @param unit the unit in which the time is expressed.
   */
  protected FixedPeriodJobTrigger(int time, final TimeUnit unit) {
    this.time = time;
    this.unit = unit;
  }
}
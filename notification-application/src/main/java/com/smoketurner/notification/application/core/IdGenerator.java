/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.core;

import com.amirkhawaja.Ksuid;
import com.smoketurner.notification.application.exceptions.NotificationStoreException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdGenerator.class);
  private final Ksuid ksuid;

  /** Constructor */
  public IdGenerator() {
    this.ksuid = new Ksuid();
  }

  /**
   * Generate a new notification ID
   *
   * @return the new notification ID
   * @throws NotificationStoreException if unable to generate an ID
   */
  public String nextId() throws NotificationStoreException {
    try {
      return ksuid.generate();
    } catch (IOException e) {
      LOGGER.error("Unable to generate new ID", e);
      throw new NotificationStoreException(e);
    }
  }
}

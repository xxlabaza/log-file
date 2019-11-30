/*
 * Copyright 2019 the original author or authors.
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

package com.xxlabaza.utils.log.file;

import com.xxlabaza.utils.log.file.exception.RecordCorruptedException;

/**
 * The handler interface for working with corrupted data.
 */
@FunctionalInterface
public interface CorruptionHandler {

  /**
   * A {@link CorruptionHandler} implementation, which just prints an exception and continuous data reading.
   */
  CorruptionHandler PRINT_STACK_TRACE_AND_CONTINUE = error -> {
    error.printStackTrace();
    return true;
  };

  /**
   * A {@link CorruptionHandler} implementation, which just prints an exception and stops data reading.
   */
  CorruptionHandler PRINT_STACK_TRACE_AND_STOP = error -> {
    error.printStackTrace();
    return false;
  };

  /**
   * A client's specified method for handling {@link RecordCorruptedException}
   * and deciding - should be file processed further or not.
   *
   * @param error the occurred data corruption error, while a file reading.
   *
   * @return {@code true} if data reading should be continued, {@code false} otherwise.
   */
  boolean handle (RecordCorruptedException error);
}

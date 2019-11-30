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

import io.appulse.utils.Bytes;

/**
 * Represents an operation that accepts a readed record from disk and a current file position offset.
 */
@FunctionalInterface
public interface RecordConsumer {

  /**
   * Performs this operation on the given arguments.
   *
   * @param record the readed record bytes from a disk.
   *
   * @param currentPosition the current file position offset.
   *
   * @return {@code true} if data reading should be continued, {@code false} otherwise.
   */
  boolean consume (Bytes record, long currentPosition);
}

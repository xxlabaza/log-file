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

package com.xxlabaza.utils.log.file.exception;

import static java.util.Locale.ENGLISH;
import static lombok.AccessLevel.PRIVATE;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * This exception may be thrown by methods that have detected corrupted data in a log file.
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class RecordCorruptedException extends RuntimeException {

  private static final long serialVersionUID = 681728085429598614L;

  long expectedChecksum;

  long calculatedChecksum;

  /**
   * Constructs a {@code RecordCorruptedException}.
   *
   * @param expectedChecksum the readed from file checksum.
   *
   * @param calculatedChecksum the calculated checksum.
   */
  public RecordCorruptedException (long expectedChecksum, long calculatedChecksum) {
    super(String.format(
        ENGLISH,
        "expected checksum (%d) doesn't equal to calculated (%d)",
        expectedChecksum, calculatedChecksum
    ));
    this.expectedChecksum = expectedChecksum;
    this.calculatedChecksum = calculatedChecksum;
  }
}

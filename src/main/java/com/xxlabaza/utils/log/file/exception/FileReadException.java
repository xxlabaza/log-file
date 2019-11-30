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

import static lombok.AccessLevel.PRIVATE;

import java.nio.file.Path;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * This is a common parent exception class for all errors, which occurs while reading a log file.
 *
 * @see FileCorruptedExcetion
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class FileReadException extends RuntimeException {

  private static final long serialVersionUID = 5083511167928041396L;

  Path file;

  /**
   * Constructs a {@code FileReadException} with a source file path.
   *
   * @param file the path to the file, where the error occurred.
   */
  public FileReadException (Path file) {
    super();
    this.file = file;
  }

  /**
   * Constructs a {@code FileReadException} with a source file path and
   * a detailed message.
   *
   * @param file the path to the file, where the error occurred.
   *
   * @param message the detail message (which is saved for later retrieval
   *                by the {@link Throwable#getMessage()} method).
   */
  public FileReadException (Path file, String message) {
    super(message);
    this.file = file;
  }

  /**
   * Constructs a {@code FileReadException} with a source file path and cause.
   *
   * @param file the path to the file, where the error occurred.
   *
   * @param cause the cause (which is saved for later retrieval by the
   *              {@link Throwable#getCause()} method).  (A {@code null} value
   *              is permitted, and indicates that the cause is nonexistent or
   *              unknown.)
   */
  public FileReadException (Path file, Throwable cause) {
    super(cause);
    this.file = file;
  }

  /**
   * Constructs a {@code FileReadException} with a source file path,
   * a detailed message and cause.
   *
   * @param file the path to the file, where the error occurred.
   *
   * @param message the detail message (which is saved for later retrieval
   *                by the {@link Throwable#getMessage()} method).
   *
   * @param cause the cause (which is saved for later retrieval by the
   *              {@link Throwable#getCause()} method).  (A {@code null} value
   *              is permitted, and indicates that the cause is nonexistent or
   *              unknown.)
   */
  public FileReadException (Path file, String message, Throwable cause) {
    super(message, cause);
    this.file = file;
  }
}

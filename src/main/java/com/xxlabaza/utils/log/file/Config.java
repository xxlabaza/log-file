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

import static io.appulse.utils.SizeUnit.KILOBYTES;

import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;

/**
 * A log file's configuration object.
 */
@With
@Value
@Builder
public class Config {

  /**
   * The configuration with default settings.
   */
  public static final Config DEFAULT = Config.builder().build();

  /**
   * A path to a log file. The default value is <b>./file.log</b>.
   *
   * @return the path to the log file.
   */
  @NonNull
  @Builder.Default
  Path path = Paths.get("./file.log");

  /**
   * A block buffer size, in bytes. It is an internal setting, which helps to
   * format the log file structure. The default value is <b>32 kilobytes</b>.
   *
   * @return the block buffer size, in bytes.
   */
  @NonNull
  @Builder.Default
  Integer blockBufferSizeBytes = (int) KILOBYTES.toBytes(32);

  /**
   * Forces any updates to the log file to be written to the storage device.
   * The default value is <b>true</b>.
   *
   * @return the current <b>forceFlush</b> value.
   */
  @NonNull
  @Builder.Default
  Boolean forceFlush = true;
}

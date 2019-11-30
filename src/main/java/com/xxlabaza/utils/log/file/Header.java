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

import java.nio.channels.FileChannel;

import io.appulse.utils.Bytes;
import io.appulse.utils.ReadBytesUtils;
import io.appulse.utils.WriteBytesUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;

@Value
@Builder
@AllArgsConstructor
class Header {

  private static final int DEFAULT_VERSION = 1;

  static final int BYTES =
      Byte.BYTES + // version
      Integer.BYTES; // block size in bytes

  @SneakyThrows
  static Header read (FileChannel channel) {
    val buffer = Bytes.allocate(BYTES);

    val position = channel.position();
    channel.position(0);

    if (ReadBytesUtils.read(channel, buffer) != BYTES) {
      throw new IllegalStateException("Invalid file's header");
    }
    if (position != 0) {
      channel.position(position);
    }

    val version = buffer.readByte();
    if (version != DEFAULT_VERSION) {
      throw new IllegalStateException("Unsupported log file version " + version);
    }
    return Header.builder()
        .version(version)
        .blockBytes(buffer.readInt())
        .build();
  }

  byte version;

  int blockBytes;

  Header (Config config) {
    version = DEFAULT_VERSION;
    blockBytes = config.getBlockBufferSizeBytes();
  }

  @SneakyThrows
  void write (FileChannel channel) {
    val buffer = Bytes.allocate(BYTES)
        .write1B(version)
        .write4B(blockBytes);

    val position = channel.position();
    channel.position(0);
    WriteBytesUtils.write(channel, buffer);
    if (position != 0) {
      channel.position(position);
    }
  }
}

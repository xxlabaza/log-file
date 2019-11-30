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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static lombok.AccessLevel.PRIVATE;

import java.nio.channels.FileChannel;

import io.appulse.utils.Bytes;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
final class Appender implements AutoCloseable {

  FileChannel channel;

  boolean forceFlush;

  Block block;

  @SneakyThrows
  Appender (Config config) {
    forceFlush = config.getForceFlush();

    channel = FileChannel.open(config.getPath(), CREATE, WRITE, READ);
    if (channel.size() == 0) {
      val header = new Header(config);
      header.write(channel);
      block = new Block(config.getBlockBufferSizeBytes());
    } else {
      val header = Header.read(channel);
      block = new Block(header.getBlockBytes());

      val size = channel.size();
      channel.position(size);

      val blockOffset = (size - Header.BYTES) % header.getBlockBytes();
      block.seek((int) blockOffset);
    }
  }

  @Override
  @SneakyThrows
  public void close () {
    block.flush(channel);
    channel.close();
  }

  @SneakyThrows
  void reset () {
    block.reset();
    channel.truncate(Header.BYTES);
  }

  @SneakyThrows
  long append (Bytes record) {
    boolean continueWrite;
    do {
      continueWrite = block.write(record);
      block.flush(channel);

      if (forceFlush) {
        channel.force(false);
      }
    } while (continueWrite);
    return channel.position();
  }
}

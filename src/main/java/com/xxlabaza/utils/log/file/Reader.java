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

import static com.xxlabaza.utils.log.file.Record.Type.FIRST;
import static com.xxlabaza.utils.log.file.Record.Type.FULL;
import static com.xxlabaza.utils.log.file.Record.Type.LAST;
import static com.xxlabaza.utils.log.file.Record.Type.UNDEFINED;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ConcurrentModificationException;

import com.xxlabaza.utils.log.file.exception.FileReadException;
import com.xxlabaza.utils.log.file.exception.RecordCorruptedException;

import io.appulse.utils.Bytes;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
final class Reader {

  LogFile logFile;

  Path file;

  Block block;

  Bytes recordBuffer;

  @Builder
  @SneakyThrows
  Reader (LogFile logFile, Config config) {
    this.logFile = logFile;
    file = config.getPath();

    try (val channel = FileChannel.open(config.getPath(), CREATE, READ, WRITE)) {
      if (channel.size() == 0) {
        val header = new Header(config);
        header.write(channel);
        block = new Block(config.getBlockBufferSizeBytes());
      } else {
        val header = Header.read(channel);
        block = new Block(header.getBlockBytes());
      }
    }
    recordBuffer = Bytes.resizableArray();
  }

  long read (RecordConsumer consumer, CorruptionHandler corruptionHandler) {
    try {
      if (Files.notExists(file)) {
        return -1;
      }
      return read0(consumer, corruptionHandler);
    } catch (FileReadException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new FileReadException(file, "unexpected error", ex);
    }
  }

  private long read0 (RecordConsumer consumer, CorruptionHandler corruptionHandler) throws IOException {
    try (val channel = FileChannel.open(file, READ)) {
      channel.position(Header.BYTES);

      val expectedModificationCount = logFile.getModificationCount();
      while (block.hasContent() == true || block.loadNext(channel) == true) {
        val currentModificationCount = logFile.getModificationCount();
        if (expectedModificationCount != currentModificationCount) {
          throw new ConcurrentModificationException();
        }
        if (readRecord(channel, corruptionHandler) == false) {
          break;
        }
        if (consumer.consume(recordBuffer, channel.position()) == false) {
          break;
        }
        recordBuffer.reset();
      }
      return channel.position();
    }
  }

  @SuppressWarnings({
      "PMD.AvoidInstantiatingObjectsInLoops",
      "PMD.UnusedPrivateMethod"
  })
  @SneakyThrows
  private boolean readRecord (FileChannel channel, CorruptionHandler corruptionHandler) {
    while (true) {
      if (block.hasContent() == false && block.loadNext(channel) == false) {
        throw new FileReadException(file, "unexpected end of file");
      }

      try {
        val type = block.read(recordBuffer);
        if (type == UNDEFINED) {
          return false;
        }
        if (type == FULL || type == LAST) {
          return true;
        }
      } catch (RecordCorruptedException ex) {
        if (corruptionHandler.handle(ex) == false) {
          return false;
        }
        if (moveToNextRecord(channel) == false) {
          return false;
        }
      } catch (Exception ex) {
        throw new FileReadException(file, ex);
      }
    }
  }

  private boolean moveToNextRecord (FileChannel channel) {
    while (true) {
      val hasBlock = block.loadNext(channel);
      if (hasBlock == false) {
        return false;
      }
      val hasRecord = block.moveTo(FIRST, FULL);
      if (hasRecord == true) {
        return true;
      }
    }
  }
}

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
import static com.xxlabaza.utils.log.file.Record.Type.MIDDLE;
import static com.xxlabaza.utils.log.file.Record.Type.UNDEFINED;
import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;

import java.nio.channels.FileChannel;
import java.util.HashSet;

import com.xxlabaza.utils.log.file.Record.Type;

import io.appulse.utils.Bytes;
import io.appulse.utils.BytesPool.PooledBytes;
import io.appulse.utils.HexUtil;
import io.appulse.utils.ReadBytesUtils;
import io.appulse.utils.WriteBytesUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
final class Block implements AutoCloseable {

  @NonNull
  PooledBytes buffer;

  @Override
  public void close () {
    buffer.release();
  }

  @Override
  public String toString () {
    val bytes = getBytes();
    return HexUtil.prettyHexDump(bytes);
  }

  byte[] getBytes () {
    return buffer.arrayCopy();
  }

  void reset () {
    buffer.reset();
  }

  boolean write (Bytes record) {
    if (buffer.isWritable() == false) {
      buffer.reset();
      return true;
    }

    val isFirstOrFullRecord = record.readerIndex() == 0;
    val isFullyFit = buffer.isWritable(Record.Header.BYTES + record.readableBytes());

    Type type;
    if (isFirstOrFullRecord) {
      type = isFullyFit
             ? FULL
             : FIRST;
    } else {
      type = isFullyFit
             ? LAST
             : MIDDLE;
    }
    write(type, record);
    return record.isReadable();
  }

  Type read (Bytes record) {
    return Record.read(record, buffer);
  }

  void flush (FileChannel channel) {
    WriteBytesUtils.write(channel, buffer);
    if (buffer.isWritable() == false) {
      reset();
    }
  }

  void seek (int offset) {
    buffer.writerIndex(offset);
    buffer.readerIndex(offset);
  }

  int capacity () {
    return buffer.capacity();
  }

  boolean hasContent () {
    return buffer.isReadable(Record.Header.BYTES + 1);
  }

  @SneakyThrows
  boolean loadNext (FileChannel channel) {
    if (buffer.readerIndex() == 0 && buffer.writerIndex() == 0) {
      return ReadBytesUtils.read(channel, buffer) > 0;
    }

    val currentPosition = channel.position();
    val nextBlockPosition = currentPosition + buffer.writableBytes();
    if (nextBlockPosition >= channel.size()) {
      return false;
    }
    channel.position(nextBlockPosition);

    buffer.reset();
    val readed = ReadBytesUtils.read(channel, buffer);
    return readed > 0;
  }

  boolean moveTo (Type... types) {
    val searchTypes = new HashSet<>(asList(types));
    while (true) {
      val type = Record.getType(buffer);
      if (searchTypes.contains(type)) {
        return true;
      }
      if (type == UNDEFINED) {
        return false;
      }

      val recordPayloadLength = Record.getLength(buffer);
      val recordLength = Record.Header.BYTES + recordPayloadLength;
      if (buffer.isReadable(recordLength) == false) {
        return false;
      }

      val currentReaderIndex = buffer.readerIndex();
      val newReaderIndex = currentReaderIndex + recordLength;
      buffer.readerIndex(newReaderIndex);
    }
  }

  private void write (Record.Type type, Bytes record) {
    Record.write(buffer, type, record);
    align();
  }

  private void align () {
    val writableBytes = buffer.writableBytes();
    if (writableBytes > Record.Header.BYTES) {
      return;
    }
    for (int index = 0; index < writableBytes; index++) {
      buffer.write1B(0);
    }
  }
}

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

import static com.xxlabaza.utils.log.file.Record.Header.BODY_LENGTH_OFFSET;
import static com.xxlabaza.utils.log.file.Record.Header.CHECKSUM_OFFSET;
import static com.xxlabaza.utils.log.file.Record.Header.TYPE_OFFSET;
import static com.xxlabaza.utils.log.file.Record.Type.UNDEFINED;

import java.util.stream.Stream;
import java.util.zip.CRC32;

import com.xxlabaza.utils.log.file.exception.RecordCorruptedException;

import io.appulse.utils.Bytes;
import lombok.Getter;
import lombok.val;

final class Record {

  static long generateChecksum (byte[] bytes, int offset, int length) {
    val crc = new CRC32();
    crc.update(bytes, offset, length);
    return crc.getValue();
  }

  static void write (Bytes destination, Type type, Bytes from) {
    val readedBytes = Math.min(destination.writableBytes() - Header.BYTES, from.readableBytes());

    val beforeChecksumIndex = destination.writerIndex();
    destination.write4B(0); // reserve

    val afterChecksumIndex = destination.writerIndex();
    destination
        .write1B(type.getCode())
        .write2B(readedBytes)
        .writeNB(from.array(), from.readerIndex(), readedBytes);

    val checksum = generateChecksum(
        destination.array(),
        afterChecksumIndex,
        destination.writerIndex() - afterChecksumIndex
    );
    destination.set4B(beforeChecksumIndex, checksum);

    from.readerIndex(from.readerIndex() + readedBytes);
  }

  static Type read (Bytes destination, Bytes source) {
    if (source.isReadable(Header.BYTES) == false) {
      return UNDEFINED;
    }
    val type = getType(source);
    val length = getLength(source);

    val checksum = source.readUnsignedInt();
    if (checksum == 0) {
      return UNDEFINED;
    }

    val calculatedChecksum = generateChecksum(
        source.array(),
        source.readerIndex(),
        Header.BYTES - Header.CHECKSUM_BYTES + length
    );
    if (checksum != calculatedChecksum) {
      throw new RecordCorruptedException(checksum, calculatedChecksum);
    }

    val offset = source.readerIndex() + Header.TYPE_BYTES + Header.BODY_LENGTH_BYTES;
    destination.writeNB(source.array(), offset, length);
    source.readerIndex(offset + length);
    return type;
  }

  static long getChecksum (Bytes from) {
    if (from.isReadable(Header.BYTES) == false) {
      return 0;
    }
    val currentReaderIndex = from.readerIndex();
    val index = currentReaderIndex + CHECKSUM_OFFSET;
    return from.getUnsignedInt(index);
  }

  static Type getType (Bytes from) {
    if (from.isReadable(Header.BYTES) == false) {
      return UNDEFINED;
    }
    val currentReaderIndex = from.readerIndex();
    val index = currentReaderIndex + TYPE_OFFSET;
    val typeTag = from.getByte(index);
    return Record.Type.from(typeTag);
  }

  static int getLength (Bytes from) {
    if (from.isReadable(Header.BYTES) == false) {
      return 0;
    }
    val currentReaderIndex = from.readerIndex();
    val index = currentReaderIndex + BODY_LENGTH_OFFSET;
    return from.getUnsignedShort(index);
  }

  private Record () {
    throw new UnsupportedOperationException();
  }

  static final class Header {

    static final int CHECKSUM_OFFSET = 0;
    static final int CHECKSUM_BYTES = Integer.BYTES;
    static final int TYPE_OFFSET = CHECKSUM_OFFSET + CHECKSUM_BYTES;
    static final int TYPE_BYTES = Byte.BYTES;
    static final int BODY_LENGTH_OFFSET = TYPE_OFFSET + TYPE_BYTES;
    static final int BODY_LENGTH_BYTES = Short.BYTES;
    static final int BODY_OFFSET = BODY_LENGTH_OFFSET + BODY_LENGTH_BYTES;
    static final int BYTES = CHECKSUM_BYTES + TYPE_BYTES + BODY_LENGTH_BYTES;

    private Header () {
      throw new UnsupportedOperationException();
    }
  }

  enum Type {

    UNDEFINED(0x00),
    FULL(0x01),
    FIRST(0x02),
    MIDDLE(0x03),
    LAST(0x04);

    @Getter
    private final byte code;

    Type (int code) {
      this.code = (byte) code;
    }

    static Type from (byte code) {
      return Stream.of(values())
          .filter(it -> it.getCode() == code)
          .findAny()
          .orElse(UNDEFINED);
    }
  }
}

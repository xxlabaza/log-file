# Overview log-file

[![build_status](https://travis-ci.org/xxlabaza/log-file.svg?branch=master)](https://travis-ci.org/xxlabaza/log-file)
[![maven_central](https://maven-badges.herokuapp.com/maven-central/com.xxlabaza.utils/log-file/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.xxlabaza.utils/log-file)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

A Java library for working with append-only files (also known as the log files). The main methods of such files are:

- **append** - for appending arbitary data;
- **load** - for fully file read from the begining till the end.

## Usage example

```java
import java.nio.file.Paths;

import com.xxlabaza.utils.log.file.Config;
import com.xxlabaza.utils.log.file.LogFile;

import io.appulse.utils.Bytes;
import io.appulse.utils.HexUtil;


// let's instantiate a log file:
Config config = Config.builder()
    .path(Paths.get("./my.log"))
    .blockBufferSizeBytes(32)
    .forceFlush(false)
    .build();

LogFile logFile = new LogFile(config);


// than, write some data to log file
Bytes record = Bytes.resizableArray()
    .write4B(42);

logFile.append(record);


// now, read all records from the file and print them to STDOUT
logFile.load((buffer, position) -> {
  String dump = HexUtil.prettyHexDump(buffer);
  System.out.println(dump);
  return true; // true - continue reading, false - stop reading
});


// flush and close the file
logFile.close();
```

## Under the hood

A log file has the next structure:

<table>
  <thead>
    <tr>
      <th align="center" colspan="5">log file structure</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td align="center" colspan="2"><b>file header</b></td>
      <td align="center" colspan="3"><b>blocks</b></td>
    </tr>
    <tr>
      <td align="center">version</td>
      <td align="center">block size</td>
      <td align="center">block 0</td>
      <td align="center">...</td>
      <td align="center">block n</td>
    </tr>
    <tr>
      <td align="center">1 byte</td>
      <td align="center">4 bytes</td>
      <td align="center" colspan="3">blocks count * <b>block size</b></td>
    </tr>
  </tbody>
</table>

### File header

File header's description:

<dl>
  <dt>version</dt>
  <dd>The file's format version, which tells the features set is used in the file.</dd>
  <dt>block size</dt>
  <dd>The size of a block buffer, in bytes, which is used in the current file.</dd>
</dl>

### Block

Each block consist of the records set:

<table>
  <thead>
    <tr>
      <th align="center" colspan="3">block</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td align="center">record 1</td>
      <td align="center">...</td>
      <td align="center">record n</td>
    </tr>
  </tbody>
</table>

The records have the following structure:

<table>
  <thead>
    <tr>
      <th align="center" colspan="4">record</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td align="center">checksum</td>
      <td align="center">type</td>
      <td align="center">body length</td>
      <td align="center">body</td>
    </tr>
    <tr>
      <td align="center">4 bytes</td>
      <td align="center">1 byte</td>
      <td align="center">2 bytes</td>
      <td align="center">body length</td>
    </tr>
  </tbody>
</table>

<dl>
  <dt>checksum</dt>
  <dd>The checksum value, which is calculated from <b>type</b>, <b>body length</b> and <b>body</b> values.</dd>
  <dt>type</dt>
  <dd>One of the record's types:
    <ul>
      <li><b>FULL</b> - the data is presented entirely in this record;</li>
      <li><b>FIRST</b> - it is the first chunk of data;</li>
      <li><b>MIDDLE</b> - one of the middle parts of data;</li>
      <li><b>LAST</b> - the last piece of data.</li>
    </ul>
  </dd>
  <dt>body length</dt>
  <dd>The length of the next body section, in bytes.</dd>
  <dt>body</dt>
  <dd>the record's payload.</dd>
</dl>

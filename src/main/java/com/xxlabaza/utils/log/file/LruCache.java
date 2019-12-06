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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.val;

class LruCache extends LinkedHashMap<Path, LogFile> {

  private static final long serialVersionUID = 627252465946108735L;

  int maxSize;

  LruCache (int maxSize) {
    super(maxSize, 1.F, true);
    this.maxSize = maxSize;
  }

  @Override
  protected boolean removeEldestEntry (Map.Entry<Path, LogFile> eldest) {
    val shouldRemove = size() > maxSize;
    if (shouldRemove == true) {
      eldest.getValue().close();
    }
    return shouldRemove;
  }
}

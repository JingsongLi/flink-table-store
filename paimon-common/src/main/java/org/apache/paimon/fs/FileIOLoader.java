/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.fs;

import org.apache.paimon.annotation.Public;

import java.util.Collections;
import java.util.Set;

/**
 * Loader to load {@link FileIO}.
 *
 * @since 0.4.0
 */
@Public
public interface FileIOLoader {

    String getScheme();

    /**
     * Returns a set of option keys (case-insensitive) that an implementation of this FileIO
     * requires. Only when these options are included will this FileIO be selected, otherwise it
     * will fall back to HadoopFileIO or compute engine's own FileIO.
     */
    default Set<String> requiredOptions() {
        return Collections.emptySet();
    }

    FileIO load(Path path);
}

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

package org.apache.paimon.compact;

import org.apache.paimon.deletionvectors.DeletionVectorsMaintainer;
import org.apache.paimon.index.IndexFileHandler;
import org.apache.paimon.index.IndexFileMeta;

import java.util.List;
import java.util.Optional;

/** Deletion File from compaction. */
public interface CompactDeletionFile {

    Optional<IndexFileMeta> getOrCompute();

    CompactDeletionFile mergeOldFile(CompactDeletionFile old);

    void clean();

    /**
     * Used by async compaction, when compaction task is completed, deletions file will be generated
     * immediately, so when updateCompactResult, we need to merge old deletion files (just delete
     * them).
     */
    static CompactDeletionFile generateFiles(DeletionVectorsMaintainer maintainer) {
        List<IndexFileMeta> files = maintainer.writeDeletionVectorsIndex();
        if (files.size() > 1) {
            throw new IllegalStateException(
                    "Should only generate one compact deletion file, this is a bug.");
        }
        Optional<IndexFileMeta> deletionFile =
                files.isEmpty() ? Optional.empty() : Optional.of(files.get(0));
        IndexFileHandler indexFileHandler = maintainer.indexFileHandler();
        return new CompactDeletionFile() {
            @Override
            public Optional<IndexFileMeta> getOrCompute() {
                return deletionFile;
            }

            @Override
            public CompactDeletionFile mergeOldFile(CompactDeletionFile old) {
                if (deletionFile.isPresent()) {
                    old.getOrCompute().ifPresent(indexFileHandler::deleteIndexFile);
                    return this;
                }

                // no update, just use old file
                return old;
            }

            @Override
            public void clean() {
                deletionFile.ifPresent(indexFileHandler::deleteIndexFile);
            }

            @Override
            public String toString() {
                return "GeneratedFiles-" + deletionFile;
            }
        };
    }

    /** For sync compaction, only create deletion files when prepareCommit. */
    static CompactDeletionFile lazyGeneration(DeletionVectorsMaintainer maintainer) {
        return new CompactDeletionFile() {
            @Override
            public Optional<IndexFileMeta> getOrCompute() {
                return generateFiles(maintainer).getOrCompute();
            }

            @Override
            public CompactDeletionFile mergeOldFile(CompactDeletionFile old) {
                return this;
            }

            @Override
            public void clean() {
                // do nothing
            }

            @Override
            public String toString() {
                return "LazyGeneration";
            }
        };
    }
}

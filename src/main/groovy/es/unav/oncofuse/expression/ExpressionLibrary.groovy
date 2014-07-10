/**
 Copyright 2014 Mikhail Shugay (mikhail.shugay@gmail.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package es.unav.oncofuse.expression

import es.unav.oncofuse.Util
import es.unav.oncofuse.features.FeatureSumMode
import es.unav.oncofuse.features.FeatureTable
import es.unav.oncofuse.segments.GenomicLibrary
import es.unav.oncofuse.segments.Transcript

class ExpressionLibrary {
    private final Map<Tissue, FeatureTable> expressionTables = new HashMap<>()
    private final Map<String, Tissue> tissueById = new HashMap<>()
    final GenomicLibrary genomicLibrary
    final int numberOfFeatures

    ExpressionLibrary(GenomicLibrary genomicLibrary, String customLibraryFileName) {
        this(genomicLibrary)
        int nFeatures = loadLibrary(customLibraryFileName, genomicLibrary, false)
        if (this.numberOfFeatures != nFeatures) {
            throw new Exception("Additional library tables doesn't have the same number of features as basic ones")
        }
    }

    ExpressionLibrary(GenomicLibrary genomicLibrary) {
        this.numberOfFeatures = loadLibrary("common/libs.txt", genomicLibrary, true)
        this.genomicLibrary = genomicLibrary
    }

    private int loadLibrary(String libraryFileName, GenomicLibrary genomicLibrary, boolean fromResource) {
        int nFeatures = -1

        Util.getInputStream(libraryFileName, fromResource).splitEachLine("\t") { splitLine ->
            if (!splitLine[0].startsWith("#")) {
                String tissueId, tissueFamily
                (tissueId, tissueFamily) = splitLine[0..1]
                boolean tissueNormal = splitLine[2].toBoolean()
                def tissue = new Tissue(tissueId, tissueFamily, tissueNormal)
                tissueById.put(tissueId, tissue)

                String fileName = splitLine[4]
                def dataStream = Util.getInputStream(fileName, fromResource)
                def featureTable = new FeatureTable(genomicLibrary,
                        dataStream, false, FeatureSumMode.Max)

                if (nFeatures < 0) {
                    nFeatures = featureTable.nFeatures
                } else if (nFeatures != featureTable.nFeatures) {
                    throw new Exception("All tables in expression library should have the same number of features")
                }

                expressionTables.put(tissue, featureTable)
            }
        }

        nFeatures
    }

    double[] getExpression(Tissue tissue, Transcript transcript) {
        expressionTables[tissue].features(transcript)
    }

    Tissue getById(String tissueId) {
        tissueById[tissueId]
    }
}

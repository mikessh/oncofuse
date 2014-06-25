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
    final Map<Tissue, FeatureTable> expressionTables = new HashMap<>()

    ExpressionLibrary(GenomicLibrary genomicLibrary, String customLibraryFileName) {
        this(genomicLibrary)
        loadLibrary(customLibraryFileName, genomicLibrary, false)
    }

    ExpressionLibrary(GenomicLibrary genomicLibrary) {
        loadLibrary("common/libs.txt", genomicLibrary, true)
    }

    void loadLibrary(String libraryFileName, GenomicLibrary genomicLibrary, boolean fromResource) {
        Util.getInputStream(libraryFileName, fromResource).splitEachLine("\t") { splitLine ->
            if (!splitLine[0].startsWith("#")) {
                //Tissue(String id, String family, boolean normal) {
                String tissueId, tissueFamily
                (tissueId, tissueFamily) = splitLine[0..1]
                boolean tissueNormal = splitLine[2].toBoolean()
                def tissue = new Tissue(tissueId, tissueFamily, tissueNormal)

                String fileName = splitLine[4]
                def dataStream = Util.getInputStream(fileName, fromResource)
                expressionTables.put(tissue, new FeatureTable(genomicLibrary,
                        dataStream, false, FeatureSumMode.Max))
            }
        }
    }

    double[] expression(Tissue tissue, Transcript transcript) {
        expressionTables[tissue].features(transcript)
    }
}

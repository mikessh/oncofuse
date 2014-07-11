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

package es.unav.oncofuse.utr

import es.unav.oncofuse.Util
import es.unav.oncofuse.features.FeatureSumMode
import es.unav.oncofuse.features.FeatureTable
import es.unav.oncofuse.segments.GenomicLibrary
import es.unav.oncofuse.segments.Transcript

class UtrFeaturesLibrary {
    private final FeatureTable utrFeatureTable
    final GenomicLibrary genomicLibrary

    UtrFeaturesLibrary(GenomicLibrary genomicLibrary) {
        this(genomicLibrary, "common/utr.txt", true)
    }

    UtrFeaturesLibrary(GenomicLibrary genomicLibrary, String fileName) {
        this(genomicLibrary, fileName, false)
    }

    private UtrFeaturesLibrary(GenomicLibrary genomicLibrary,
                               String inputFileName, boolean fromResource) {
        this.genomicLibrary = genomicLibrary
        this.utrFeatureTable = new FeatureTable(genomicLibrary,
                Util.getInputStream(inputFileName, fromResource), false, FeatureSumMode.Avg)
    }

    double[] utrFeatures(Transcript transcript) {
        utrFeatureTable.features(transcript)
    }

    int getNumberOfFeatures() {
        utrFeatureTable.numberOfFeatures
    }
}

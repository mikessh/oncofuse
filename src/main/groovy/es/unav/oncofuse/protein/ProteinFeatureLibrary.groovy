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

package es.unav.oncofuse.protein

import es.unav.oncofuse.Util
import es.unav.oncofuse.segments.GenomicLibrary
import es.unav.oncofuse.segments.Transcript

class ProteinFeatureLibrary {
    final Map<Transcript, ProteinFeatures> transcript2Features = new HashMap<>()
    final GenomicLibrary genomicLibrary

    ProteinFeatureLibrary(GenomicLibrary genomicLibrary) {
        this.genomicLibrary = genomicLibrary

        Util.loadResource("common/domains.txt").eachLine(1) { line ->
            //ENSTxxxx KANSL1L	IPR026180	1	987	Family
            def splitLine = line.split("\t")
            String transcriptId, geneName, domainId, domainType
            (transcriptId, geneName, domainId, domainType) = splitLine[[(0..2), 5].flatten()]
            int aaFrom, aaTo
            (aaFrom, aaTo) = splitLine[3..4].collect { it.toInteger() }

            def features = getFeatures(transcriptId, geneName)
            if (features)
                features.addFeature(new Domain(aaFrom, aaTo, domainType, domainId))
        }

        Util.loadResource("common/pii.txt").eachLine(1) { line ->
            //fromENST  A2M	toENST  KLK3	1376	1463
            def splitLine = line.split("\t")
            String fromTranscriptId, fromGeneName, toTranscriptId, toGeneName
            (fromTranscriptId, fromGeneName, toTranscriptId, toGeneName) = splitLine[0..3]
            int aaFrom, aaTo
            (aaFrom, aaTo) = splitLine[4..5].collect { it.toInteger() }

            def target = genomicLibrary.fetchTranscript(toTranscriptId, toGeneName)
            if (target) {
                def features = getFeatures(fromTranscriptId, fromGeneName)
                if (features)
                    features.addFeature(new Pii(aaFrom, aaTo, target))
            }
        }
    }

    private ProteinFeatures getFeatures(String transcriptId, String geneName) {
        def transcript = genomicLibrary.fetchTranscript(transcriptId, geneName)
        if (transcript) {
            def features = transcript2Features[transcript]
            if (features == null)
                transcript2Features.put(transcript, features = new ProteinFeatures(transcript))
            return features
        }
        null
    }
}

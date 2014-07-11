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

package es.unav.oncofuse.features

import es.unav.oncofuse.segments.GenomicLibrary
import es.unav.oncofuse.segments.Transcript

class FeatureTable {
    private final Map<Transcript, FeatureData> featureMap = new HashMap<>()
    final int numberOfFeatures

    FeatureTable(GenomicLibrary genomicLibrary,
                 InputStreamReader input,
                 boolean canonicalOnly,
                 FeatureSumMode accumulationMode) {
        int nFeatures = -1
        input.splitEachLine("\t") { splitLine ->
            if (!splitLine[0].startsWith("#")) {
                String transcriptId, geneName
                (transcriptId, geneName) = splitLine[0..1]
                def transcript = canonicalOnly ? genomicLibrary.fetchCanonicalTranscript(geneName) :
                        genomicLibrary.fetchTranscript(transcriptId, geneName)

                def featureData = featureMap[transcript]

                if (nFeatures < 0) {
                    nFeatures = splitLine.size() - 2
                } else if (nFeatures != splitLine.size() - 2) {
                    throw new Exception("Same number of features expected for each row of feature table")
                }

                if (nFeatures == 0)
                    throw new Exception("Table doesn't contain any features")

                if (!featureData)
                    featureMap.put(transcript, featureData = new FeatureData(splitLine))

                featureData."${accumulationMode.value()}"(splitLine)
            }
        }

        this.numberOfFeatures = nFeatures

        if (accumulationMode == FeatureSumMode.Avg)
            featureMap.values().each { it.avg() }
    }

    double[] features(Transcript transcript) {
        def featureData = featureMap[transcript]
        if (featureData)
            return featureData.values
        (0..<numberOfFeatures).collect { Double.NaN }
    }

    private class FeatureData {
        final double[] values
        int count = 0

        FeatureData(List<String> splitLine) {
            this.values = new double[splitLine.size() - 2]
        }

        void max(List<String> splitLine) {
            splitLine[2..-1].eachWithIndex { it, ind ->
                values[ind] = Math.max(values[ind], it.toDouble())
            }
            count++
        }

        void sum(List<String> splitLine) {
            splitLine[2..-1].eachWithIndex { it, ind ->
                values[ind] += it.toDouble()
            }
            count++
        }

        void avg(List<String> splitLine) {
            sum(splitLine)
        }

        void avg() {
            for (int i = 0; i < values.length; i++)
                values[i] /= count
        }
    }
}

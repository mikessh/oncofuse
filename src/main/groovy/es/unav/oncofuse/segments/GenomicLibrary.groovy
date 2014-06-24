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

package es.unav.oncofuse.segments

import es.unav.oncofuse.GeneSystem
import es.unav.oncofuse.Util

class GenomicLibrary {
    private final Map<String, Map<Integer, List<Transcript>>> transcript2Bin2Chom = new HashMap<>()
    private final Map<String, List<Transcript>> transcript2Name = new HashMap<>()
    private final Map<String, Transcript> transcript2Id = new HashMap<>()

    GenomicLibrary(GeneSystem system) {
        def resourceName = "common/{$system}.txt"
        Util.loadResource(resourceName).eachLine(1) { line ->
            def transcript = new Transcript(line)
            def transcripts2Bin = transcript2Bin2Chom[transcript.chr]
            if (transcripts2Bin == null)
                transcript2Bin2Chom.put(transcript.chr, transcripts2Bin = new HashMap<>())

            def bins = [bin(transcript.start),
                        bin(transcript.end)]

            if (bins[0] == bins[1])
                bins = bins[0]

            bins.each { int bin ->
                def transcripts = transcripts2Bin[bin]
                if (transcripts == null)
                    transcripts2Bin.put(bin, transcripts = new LinkedList<>())
                transcripts.add(transcript)
            }

            def transcripts = transcript2Name[transcript.name]
            if (transcripts == null)
                transcript2Name.put(transcript.name, transcripts = new LinkedList<>())
            transcripts.add(transcript)

            transcript2Id.put(transcript.id, transcript)
        }

        // Select canonical transcripts
        transcript2Name.values().each {
            it.max { it.cdsSize }.canonical = true
        }
    }

    List<Transcript> fetchTranscripts(String chr, int coord) {
        int bin = bin(coord)
        transcript2Bin2Chom[chr][bin].findAll { it.overlaps(coord) }
    }

    List<Transcript> fetchTranscripts(String geneName) {
        transcript2Name[geneName] ?: new LinkedList<>()
    }

    Transcript fetchTranscript(String id) {
        transcript2Id[id]
    }

    Transcript fetchCanonicalTranscript(String geneName) {
        fetchTranscripts(geneName).find { it.canonical }
    }

    Transcript fetchTranscript(String transcriptId, String geneName) {
        fetchTranscript(transcriptId) ?: fetchCanonicalTranscript(geneName)
    }

    private static int bin(int coord) {
        coord / 500000
    }
}

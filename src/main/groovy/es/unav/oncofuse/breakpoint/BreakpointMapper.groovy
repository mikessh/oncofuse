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

package es.unav.oncofuse.breakpoint

import es.unav.oncofuse.segments.GenomicLibrary
import es.unav.oncofuse.segments.SegmentType
import es.unav.oncofuse.segments.Transcript

class BreakpointMapper {
    private final GenomicLibrary genomicLibrary

    BreakpointMapper(GenomicLibrary genomicLibrary) {
        this.genomicLibrary = genomicLibrary
    }

    List<TranscriptBreakpoint> map(GenomicBreakpoint breakpoint) {
        def transcriptBreakpoints = new LinkedList<TranscriptBreakpoint>()

        // Find corresponding transcript
        def transcripts = genomicLibrary.fetchTranscripts(breakpoint.chr, breakpoint.coord)

        transcripts.each { Transcript transcript ->
            def segment = transcript.fetchSegment(breakpoint.coord)
            int segmentGenomicCoord = (transcript.strand ?
                    breakpoint.coord - segment.start :
                    segment.end - breakpoint.coord) + 1

            //
            //
            // ---------->...|......--------->
            //           e   |      s
            //

            int retainedCdsSize = segment.upstreamCdsSize

            if (segment.type == SegmentType.Exon && segment.cds.cdsSize > 0)
                retainedCdsSize += (transcript.strand ?
                        breakpoint.coord - segment.cds.startAbs :
                        segment.cds.endAbs - breakpoint.coord)

            def breakpointType
            if (retainedCdsSize == 0)  // 1-based
                breakpointType = BreakpointType.UTR5
            else if (retainedCdsSize == transcript.cdsSize)  // inclusive
                breakpointType = BreakpointType.UTR3
            else
                breakpointType = BreakpointType.CDS

            transcriptBreakpoints.add(new TranscriptBreakpoint(breakpoint, segment, breakpointType,
                    segmentGenomicCoord, retainedCdsSize,
                    breakpoint.prime5 ? retainedCdsSize : transcript.cdsSize - retainedCdsSize
            ))
        }

        transcriptBreakpoints
    }
}

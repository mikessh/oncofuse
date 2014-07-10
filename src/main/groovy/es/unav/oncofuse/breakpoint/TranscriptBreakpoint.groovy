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

import es.unav.oncofuse.segments.Segment
import es.unav.oncofuse.segments.Transcript

class TranscriptBreakpoint {
    final GenomicBreakpoint genomicBreakpoint
    final Transcript parentTranscript
    final BreakpointType breakpointType
    final Segment segment
    final int coordInSegment, coordInCds, retainedCdsSize
    final int retainedProteinPart, frame

    TranscriptBreakpoint(GenomicBreakpoint genomicBreakpoint, Transcript parentTranscript,
                         Segment segment, BreakpointType breakpointType,
                         int coordInSegment, coordInCds, int retainedCdsSize) {
        this.genomicBreakpoint = genomicBreakpoint
        this.parentTranscript = parentTranscript
        this.segment = segment
        this.breakpointType = breakpointType
        this.coordInSegment = coordInSegment
        this.coordInCds = coordInCds
        this.retainedCdsSize = retainedCdsSize
        this.retainedProteinPart = retainedCdsSize / 3
        this.frame = retainedCdsSize - this.retainedProteinPart
    }
}

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

package es.unav.oncofuse.fusion

import es.unav.oncofuse.breakpoint.BreakpointMapper
import es.unav.oncofuse.breakpoint.GenomicBreakpoint
import es.unav.oncofuse.sample.SampleMetadata
import es.unav.oncofuse.segments.GenomicLibrary

class FusionBuilder {
    private final BreakpointMapper breakpointMapper
    final SampleMetadata sampleMetadata

    FusionBuilder(GenomicLibrary genomicLibrary, SampleMetadata sampleMetadata) {
        this.breakpointMapper = new BreakpointMapper(genomicLibrary)
        this.sampleMetadata = sampleMetadata
    }

    FusionData create(GenomicBreakpoint breakpoint5, GenomicBreakpoint breakpoint3) {
        def fpg5 = new FpgData(breakpointMapper.map(breakpoint5, true)),
            fpg3 = new FpgData(breakpointMapper.map(breakpoint3, false))
        new FusionData(fpg5, fpg3, sampleMetadata)
    }
}

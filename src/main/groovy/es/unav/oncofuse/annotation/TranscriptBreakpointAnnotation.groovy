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

package es.unav.oncofuse.annotation

import es.unav.oncofuse.breakpoint.TranscriptBreakpoint
import es.unav.oncofuse.protein.ProteinFeature

class TranscriptBreakpointAnnotation {
    final TranscriptBreakpoint fpg5tr, fpg3tr
    final NaiveFeatures retained, lost
    final final List<ProteinFeature> spanningFeatures

    TranscriptBreakpointAnnotation(TranscriptBreakpoint fpg5tr, TranscriptBreakpoint fpg3tr,
                                   NaiveFeatures retained, NaiveFeatures lost,
                                   List<ProteinFeature> spanningFeatures) {
        this.fpg5tr = fpg5tr
        this.fpg3tr = fpg3tr
        this.retained = retained
        this.lost = lost
        this.spanningFeatures = spanningFeatures
    }
}

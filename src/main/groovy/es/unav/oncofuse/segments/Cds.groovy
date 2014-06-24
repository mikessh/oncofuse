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

class Cds {
    final CdsType segment
    final int startAbs, endAbs, startRel, endRel, cdsSize

    Cds(int genomicStart, int genomicEnd, int upstreamCdsSize,
        int cdsStart, int cdsEnd, boolean strand) {
        int genomicCdsStart = Math.max(genomicStart, cdsStart),
            genomicCdsEnd = Math.min(genomicEnd, cdsEnd)

        //
        // cds        <-------->
        //      <||||>
        //          <||-->
        //               <---->
        //                   <--||>
        //                      <||||>

        // exon
        // s     e
        // <----->
        //
        // intron
        //
        //       e       s
        // <----->.......<---->

        boolean utr5 = genomicCdsStart > genomicStart, utr3 = genomicCdsEnd < genomicEnd
        if (genomicCdsStart > genomicEnd || genomicCdsEnd < genomicStart) {
            segment = utr3 && strand ? CdsType.UTR3 : CdsType.UTR5
            this.startRel = -1
            this.endRel = -1
            this.startAbs = -1
            this.endAbs = -1
        } else {
            if (utr5)
                segment = CdsType.UTR5CDS
            else if (utr3)
                segment = CdsType.CDSUTR3
            else
                segment = CdsType.CDS

            //
            //  123 456789
            //  AAA AAAAAA xxx
            //      4    9
            //

            this.startRel = upstreamCdsSize + 1
            this.endRel = this.startRel + genomicCdsEnd - genomicCdsStart
            this.startAbs = genomicCdsStart
            this.endAbs = genomicCdsEnd
        }

        this.cdsSize = startRel < 0 ? 0 : (endRel - startRel + 1) // 1-based, inclusive
    }
}

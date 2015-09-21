/*
 * Copyright 2013-2014 Mikhail Shugay (mikhail.shugay@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last modified on 22.11.2014 by mikesh
 */


package es.unav.oncofuse.legacy


class FpgPart {
    final String chrom, geneName, signature
    final int chrCoord
    final boolean fivePrimeFpg, exon, cds
    final int segmentId, coord, aaPos, frame, fullLen

    FpgPart(String chrom, int chrCoord, String geneName, boolean fivePrimeFpg, boolean exon, int segmentId, int coord, int aaPos, int frame, int fullLen) {
        this.chrom = chrom
        this.chrCoord = chrCoord
        this.geneName = geneName
        this.fivePrimeFpg = fivePrimeFpg
        this.cds = aaPos > 0 && aaPos < fullLen && coord >= 0
        this.exon = exon
        this.segmentId = segmentId
        this.coord = coord
        this.aaPos = aaPos
        this.frame = frame
        this.fullLen = fullLen
        this.signature = "$geneName\t$fivePrimeFpg\t$exon\t$segmentId\t$coord"
    }

    String toPrettyString() {
        return [geneName, cds ? "Yes" : "No", exon ? "Exon" : "Intron", segmentId, coord, fivePrimeFpg ? aaPos : fullLen - aaPos, frame].join("\t")
    }

    @Override
    String toString() {
        return signature
    }

    @Override
    int hashCode() {
        return this.signature.hashCode()
    }

    @Override
    boolean equals(Object obj) {
        return this.signature.equals((obj as FpgPart).signature)
    }
}
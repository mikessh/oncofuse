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

class Transcript {
    final boolean strand
    final int start, end, cdsStart, cdsEnd, cdsSize
    final List<Segment> segments = new ArrayList<>()
    final String id, name, chr
    boolean canonical = false

    Transcript(String refseqLine) {
        // Refseq canonical data schema
        // 0        1       2       3       4       5           6       7           8           9
        // #name	chrom	strand	txStart	txEnd	cdsStart	cdsEnd	exonStarts	exonEnds	name2

        def splitLine = refseqLine.split("\t")

        (id, chr, name) = splitLine[[0, 1, 9]]
        strand = splitLine[2] == "+"
        (start, end, cdsStart, cdsEnd) = splitLine[3..6].collect { it.toInteger() }

        def exonStarts = splitLine[7].split(",").collect { it.toInteger() },
            exonEnds = splitLine[8].split(",").collect { it.toInteger() }

        final int nExons = exonStarts.size()
        int upstreamCdsSize = 0

        //
        //   0 1 2 3 4 5   exon index
        //
        //   1 2 3 4 5 6   exon id   (+)
        //    1 2 3 4 5    intron id (+)
        //
        //   5 4 3 2 1 0   exon index
        //
        //   6 5 4 3 2 1   exon id   (-)
        //    5 4 3 2 1    intron id (-)

        (strand ? (0..<nExons) : ((nExons - 1)..0)).each { int i ->
            int exonId = strand ? i + 1 : nExons - i

            def segment = new Segment(exonId,
                    SegmentType.Exon,
                    exonStarts[i], exonEnds[i],
                    upstreamCdsSize, this)
            segments.add(segment)

            upstreamCdsSize += segment.cdsSize

            if (exonId < nExons) {
                segments.add(new Segment(exonId,
                        SegmentType.Intron,
                        strand ? exonEnds[i] + 1 : exonStarts[i] - 1,
                        strand ? exonStarts[i + 1] - 1 : exonEnds[i - 1] + 1,
                        upstreamCdsSize, this))
            }
        }

        cdsSize = upstreamCdsSize
    }

    boolean overlaps(int coord) {
        start <= coord && end >= coord // 1-based inclusive
    }

    Segment fetchSegment(int coord) {
        segments.find { it.overlaps(coord) }
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Transcript that = (Transcript) o

        if (id != that.id) return false

        return true
    }

    int hashCode() {
        return id.hashCode()
    }
}

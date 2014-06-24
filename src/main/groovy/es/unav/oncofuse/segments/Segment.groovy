package es.unav.oncofuse.segments
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
class Segment {
    final Cds cds
    final int id, start, end, upstreamCdsSize
    final Transcript parent
    final SegmentType type

    Segment(int id, SegmentType type,
            int start, int end, int upstreamCdsSize, Transcript parent) {
        this.id = id
        this.type = type
        this.parent = parent
        this.start = start
        this.end = end
        this.upstreamCdsSize = upstreamCdsSize
        this.cds = type == SegmentType.Exon ? new Cds(start, end, upstreamCdsSize,
                parent.cdsStart, parent.cdsEnd, parent.strand) : null
    }

    int getCdsSize() {
        type == SegmentType.Exon ? cds.cdsSize : 0
    }

    boolean overlaps(int coord) {
        start <= coord && end >= coord // 1-based, inclusive
    }
}

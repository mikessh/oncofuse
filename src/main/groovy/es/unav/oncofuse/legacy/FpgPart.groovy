package es.unav.oncofuse.legacy


class FpgPart {
    String chrom
    int chrCoord
    String geneName
    boolean fivePrimeFpg
    boolean exon
    int segmentId, coord, aaPos, frame, fullLen
    boolean cds
    String signature

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
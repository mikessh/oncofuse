package es.unav.oncofuse.legacy


class Fusion {
    final int nSpan, nEncomp
    final FpgPart fpgPart5, fpgPart3
    final String tissue, sample
    final int id

    Fusion(int id, int nSpan, int nEncomp,
           FpgPart fpgPart5, FpgPart fpgPart3,
           String tissue, String sample) {
        this.id = id
        this.nSpan = nSpan
        this.nEncomp = nEncomp
        this.fpgPart5 = fpgPart5
        this.fpgPart3 = fpgPart3
        this.tissue = tissue
        this.sample = sample
    }
}
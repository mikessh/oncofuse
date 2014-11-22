package es.unav.oncofuse.legacy


class Feature {
    String parentGene
    int aaFrom, aaTo
    String featureId

    Feature(String parentGene, int aaFrom, int aaTo, String featureId) {
        this.parentGene = parentGene
        this.aaFrom = aaFrom
        this.aaTo = aaTo
        this.featureId = featureId
    }
}
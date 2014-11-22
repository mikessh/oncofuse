package es.unav.oncofuse.legacy


class DomainFeatures {
    // Stage 1:
    def domainsRetained = new HashSet<String>(), domainsLost = new HashSet<String>(), domainsBroken = new HashSet<String>()
    def piisRetained = new ArrayList<String>(), piisLost = new ArrayList<String>()

    // Stage 2:
    int nSelfPIIsRetained = 0, nSelfPIIsLost = 0
    int nPIIsRetained, nPIIsLost
    double[] domainProfileRetained, domainProfileLost, domainProfileBroken
}
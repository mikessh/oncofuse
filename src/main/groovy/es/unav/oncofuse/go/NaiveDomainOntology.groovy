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

package es.unav.oncofuse.go

import es.unav.oncofuse.protein.Domain
import es.unav.oncofuse.protein.ProteinFeatureLibrary

class NaiveDomainOntology implements DomainOntology {
    private final Map<String, List<GoTerm>> domainId2Go = new HashMap<>()
    final GeneOntology geneOntology
    final ProteinFeatureLibrary proteinFeatureLibrary

    NaiveDomainOntology(ProteinFeatureLibrary proteinFeatureLibrary) {
        this(proteinFeatureLibrary, new NaiveGeneOntology(true))
    }

    NaiveDomainOntology(ProteinFeatureLibrary proteinFeatureLibrary,
                        GeneOntology geneOntology) {
        this.proteinFeatureLibrary = proteinFeatureLibrary
        this.geneOntology = geneOntology
        proteinFeatureLibrary.transcript2Features.each {
            if (it.value instanceof Domain && it.key.canonical) {
                def domainId = (it.value as Domain).id

                def goList = domainId2Go[domainId]
                if (!goList)
                    domainId2Go.put(domainId, goList = new LinkedList<GoTerm>())
                goList.addAll(geneOntology.goTerms(it.key))
            }
        }
    }

    List<GoTerm> goTerms(Domain domain) {
        domainId2Go[domain.id] ?: new LinkedList<>()
    }

    int getNumberOfThemes() {
        geneOntology.numberOfThemes
    }

    int domainAbundance(Domain domain) {
        proteinFeatureLibrary.domainCounts[domain.id]
    }
}

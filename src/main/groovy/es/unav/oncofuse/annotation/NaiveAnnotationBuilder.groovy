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

import es.unav.oncofuse.expression.ExpressionLibrary
import es.unav.oncofuse.go.DomainOntology
import es.unav.oncofuse.protein.Domain
import es.unav.oncofuse.protein.ProteinFeatureLibrary
import es.unav.oncofuse.segments.GenomicLibrary
import es.unav.oncofuse.utr.UtrFeaturesLibrary

class NaiveAnnotationBuilder {
    final GenomicLibrary genomicLibrary
    final ExpressionLibrary expressionLibrary
    final UtrFeaturesLibrary utrFeaturesLibrary
    final ProteinFeatureLibrary proteinFeatureLibrary
    final DomainOntology domainOntology

    NaiveAnnotationBuilder(GenomicLibrary genomicLibrary, ExpressionLibrary expressionLibrary,
                    UtrFeaturesLibrary utrFeaturesLibrary, DomainOntology domainOntology) {
        this.genomicLibrary = genomicLibrary
        this.expressionLibrary = expressionLibrary
        this.utrFeaturesLibrary = utrFeaturesLibrary
        this.proteinFeatureLibrary = domainOntology.proteinFeatureLibrary
        this.domainOntology = domainOntology
    }



    private void appendFFAS(double[] ffas, Domain domain) {
        def goTerms = domainOntology.goTerms(domain)
        double factor = 1.0 / domainOntology.domainAbundance(domain)

        goTerms.each {
            it.themes.each {
                ffas[it.id] += factor
            }
        }
    }
}

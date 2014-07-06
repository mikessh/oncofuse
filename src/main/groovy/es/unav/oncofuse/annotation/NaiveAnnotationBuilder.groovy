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
import es.unav.oncofuse.expression.Tissue
import es.unav.oncofuse.fusion.FusionData
import es.unav.oncofuse.go.DomainOntology
import es.unav.oncofuse.protein.Domain
import es.unav.oncofuse.protein.Pii
import es.unav.oncofuse.protein.ProteinFeature
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

    NaiveAnnotation annotate(FusionData fusion) {
        def tissue = fusion.sample.tissue

        def annotations = new LinkedList<TranscriptBreakpointAnnotation>()

        fusion.fpg5.parents.each { fpg5tr ->
            fusion.fpg3.parents.each { fpg3tr ->
                def expr5 = expressionLibrary.expression(tissue, fpg5tr.parentTranscript),
                    expr3 = expressionLibrary.expression(tissue, fpg3tr.parentTranscript)

                def utr5 = utrFeaturesLibrary.utrFeatures(fpg5tr.parentTranscript),
                    utr3 = utrFeaturesLibrary.utrFeatures(fpg3tr.parentTranscript)

                def features5R = proteinFeatureLibrary.fetchFeatures(fpg5tr.parentTranscript, fpg5tr.coordInCds, true),
                    features3R = proteinFeatureLibrary.fetchFeatures(fpg3tr.parentTranscript, fpg3tr.coordInCds, false),
                    features5L = proteinFeatureLibrary.fetchFeatures(fpg5tr.parentTranscript, fpg5tr.coordInCds, false),
                    features3L = proteinFeatureLibrary.fetchFeatures(fpg3tr.parentTranscript, fpg3tr.coordInCds, true),
                    features5S = proteinFeatureLibrary.fetchSpanningFeatures(fpg5tr.parentTranscript, fpg5tr.coordInCds),
                    features3S = proteinFeatureLibrary.fetchSpanningFeatures(fpg3tr.parentTranscript, fpg3tr.coordInCds)

                def featuresR = [features5R.features, features3R.features].flatten(),
                    featuresL = [features5L.features, features3L.features].flatten(),
                    featuresS = [features5S.features, features3S.features].flatten()


                double[] ffasR = new double[domainOntology.numberOfThemes],
                         piiExprR = new double[expressionLibrary.numberOfFeatures],
                         ffasL = new double[domainOntology.numberOfThemes],
                         piiExprL = new double[expressionLibrary.numberOfFeatures]


                featuresR.each { ProteinFeature feature ->
                    appendFeature(ffasR, piiExprR, feature, tissue)
                }

                featuresL.each { ProteinFeature feature ->
                    appendFeature(ffasL, piiExprL, feature, tissue)
                }

                def annotation = new TranscriptBreakpointAnnotation(fpg5tr, fpg3tr,
                        new NaiveFeatures(expr5, ffasR, piiExprR, utr3, featuresR),
                        new NaiveFeatures(expr3, ffasL, piiExprL, utr5, featuresL),
                        featuresS)

                annotations.add(annotation)
            }
        }

        new NaiveAnnotation(fusion, annotations)
    }

    private void appendFeature(double[] ffas, double[] piiExpr, ProteinFeature feature, Tissue tissue) {
        if (feature instanceof Domain)
            appendFFAS(ffas, feature as Domain)
        else if (feature instanceof Pii)
            appendPiiExpr(piiExpr, tissue, feature as Pii)
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

    private void appendPiiExpr(double[] piiExpr, Tissue tissue, Pii pii) {
        def target = pii.target
        def features = expressionLibrary.expression(tissue, target)
        if (features) {
            for (int i = 0; i < piiExpr.length; i++)
                piiExpr[i] += features[i]
        }
    }
}

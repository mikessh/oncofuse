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

import es.unav.oncofuse.protein.ProteinFeature

class BasicFeatures {
    final double[] expr, ffas, pii, utr
    final List<ProteinFeature> proteinFeatures
    final int nFeatures

    BasicFeatures(double[] expr, double[] ffas, double[] pii, double[] utr,
                  List<ProteinFeature> proteinFeatures) {
        this.expr = expr
        this.ffas = ffas
        this.pii = pii
        this.utr = utr
        this.nFeatures = expr.size() + ffas.size() + pii.size() + utr.size()
        this.proteinFeatures = proteinFeatures
    }

    List<String> listFeatureNames() {
        [(0..expr.length).collect { "EXPR_$it" },
         (0..ffas.length).collect { "FFAS_$it" },
         (0..pii.length).collect { "PII_EXPR_$it" },
         (0..utr.length).collect { "UTR_$it" }].flatten()
    }

    List<Double> listFeatures() {
        [expr, ffas.collect { Math.log1p(it) }, pii.collect { Math.log1p(it) }, utr].flatten()
    }
}

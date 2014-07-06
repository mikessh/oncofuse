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

package es.unav.oncofuse.protein

import es.unav.oncofuse.segments.Transcript

class ProteinFeatures {
    final Transcript parent
    final int aaFrom, aaTo
    private final List<ProteinFeature> features

    private ProteinFeatures(Transcript parent,
                            List<ProteinFeature> features,
                            int aaFrom, int aaTo) {
        this.parent = parent
        this.features = features
        this.aaFrom = aaFrom
        this.aaTo = aaTo
    }

    ProteinFeatures(Transcript parent) {
        this.features = new LinkedList<>()
        this.parent = parent
        this.aaFrom = 1
        this.aaTo = parent.cdsSize
    }

    void addFeature(ProteinFeature feature) {
        features.add(feature)
    }

    ProteinFeatures forRange(int aaFrom, int aaTo) {
        new ProteinFeatures(this.parent, features.findAll { it.inRange(aaFrom, aaTo) },
                aaFrom, aaTo)
    }

    ProteinFeatures forRange(int cdsPos, boolean prime5) {
        new ProteinFeatures(parent, features.findAll { it.inRange(cdsPos, prime5) },
                prime5 ? 1 : cdsPos, prime5 ? cdsPos : parent.cdsSize)
    }

    ProteinFeatures span(int cdsPos) {
        new ProteinFeatures(parent, features.findAll { it.span(cdsPos) },
                cdsPos, cdsPos)
    }

    List<Domain> getDomains() {
        features.findAll { it instanceof Domain }
    }

    List<ProteinFeature> getFeatures() {
        features
    }

    List<Pii> getPiis() {
        features.findAll { it instanceof Pii }
    }
}

/*
 * Copyright 2013-2014 Mikhail Shugay (mikhail.shugay@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last modified on 22.11.2014 by mikesh
 */


package es.unav.oncofuse.legacy


class Feature {
    final String parentGene
    final int aaFrom, aaTo
    final String featureId

    Feature(String parentGene, int aaFrom, int aaTo, String featureId) {
        this.parentGene = parentGene
        this.aaFrom = aaFrom
        this.aaTo = aaTo
        this.featureId = featureId
    }
}
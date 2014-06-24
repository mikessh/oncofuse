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

class ProteinFeature {
    final int aaFrom, aaTo
    final String type

    ProteinFeature(int aaFrom, int aaTo, String type) {
        this.aaFrom = aaFrom
        this.aaTo = aaTo
        this.type = type
    }

    boolean inRange(int aaFrom, int aaTo) {
        this.aaFrom >= aaFrom && this.aaTo <= aaTo
    }

    boolean inRange(int cdsEnd, boolean prime5) {
        prime5 ? this.aaTo <= cdsEnd : this.aaFrom >= cdsEnd
    }
}

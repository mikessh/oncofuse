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
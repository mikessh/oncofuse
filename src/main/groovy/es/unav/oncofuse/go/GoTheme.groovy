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

class GoTheme {
    final String name
    final int id

    GoTheme(String name, int id) {
        this.name = name
        this.id = id
    }

    static GoTheme OTHER = new GoTheme("O", -1)

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        GoTheme goTheme = (GoTheme) o

        if (name != goTheme.name) return false

        return true
    }

    @Override
    int hashCode() {
        return name.hashCode()
    }
}

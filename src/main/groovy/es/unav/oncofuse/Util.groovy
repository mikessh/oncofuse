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

package es.unav.oncofuse

class Util {
    static InputStreamReader loadResource(String resourceName) {
        new InputStreamReader(Util.class.classLoader.getResourceAsStream(resourceName))
    }

    static File resourceAsFile(String resourceName) {
        def url = Util.class.classLoader.getResource(resourceName)
        new File(url.toURI())
    }

    static File getFile(String fileName, boolean fromResources) {
        fromResources ? resourceAsFile(fileName) : new File(fileName)
    }

    static InputStreamReader getInputStream(String fileName, boolean fromResources) {
        fromResources ? loadResource(fileName) :
                new InputStreamReader(new FileInputStream(fileName))
    }
}

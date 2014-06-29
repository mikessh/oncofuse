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

import es.unav.oncofuse.Util
import es.unav.oncofuse.segments.Transcript

class NaiveGeneOntology implements GeneOntology {
    private final Map<String, List<GoTerm>> geneId2Go = new HashMap<>()
    final int numberOfThemes

    NaiveGeneOntology(boolean davidExpand) {
        final Map<String, GoTerm> goTermsWithThemes = new HashMap<>()

        def themes = new HashMap<String, GoTheme>()

        Util.loadResource("common/go2theme.txt").splitEachLine("\t") { splitLine ->
            if (!splitLine[0].startsWith("#")) {
                String goId = splitLine[0], themeName = splitLine[1]

                def goTerm = goTermsWithThemes[goId]
                if (goTerm == null)
                    goTermsWithThemes.put(goId, goTerm = new GoTerm(goId))

                def theme = themes[themeName]
                if (!theme)
                    themes.put(themeName, theme = new GoTheme(themeName, themes.size()))

                goTerm.themes.add(theme)
            }
        }

        this.numberOfThemes = themes.size()

        final Map<String, List<GoTerm>> davidId2Go = new HashMap<>()
        Util.loadResource("common/id2go.txt").splitEachLine("\t") { splitLine ->
            if (!splitLine[0].startsWith("#")) {
                String davidId = splitLine[0], goId = splitLine[1]
                def goTerm = goTermsWithThemes[goId] ?: new GoTerm(goId)
                def terms = davidId2Go[davidId]
                if (!terms)
                    davidId2Go.put(davidId, terms = new LinkedList<GoTerm>())
                terms.add(goTerm)
            }
        }

        final Map<String, List<String>> davidExpandMap = new HashMap<>()
        if (davidExpand) {
            Util.loadResource("common/id_expand.txt").splitEachLine("\t") { splitLine ->
                if (!splitLine[0].startsWith("#")) {
                    String parent = splitLine[0], child = splitLine[1]
                    def davidExpandList = davidExpandMap[parent]
                    if (!davidExpandList)
                        davidExpandMap.put(parent, davidExpandList = new LinkedList<String>())
                    davidExpandList.add(child)
                }
            }
        }

        final Map<String, List<String>> geneName2DavidId = new HashMap<>()
        Util.loadResource("common/gene_symbol2id.txt").splitEachLine("\t") { splitLine ->
            if (!splitLine[0].startsWith("#")) {
                String geneName = splitLine[0], davidId = splitLine[1]
                def davidIdList = geneName2DavidId[geneName]
                if (!davidIdList)
                    geneName2DavidId.put(geneName, davidIdList = new LinkedList<String>())
                davidIdList.add(davidId)

                def davidExpandList = davidExpandMap[davidId] ?: new LinkedList<>()
                davidIdList.addAll(davidExpandList)
            }
        }

        geneName2DavidId.each {
            String geneName = it.key
            it.value.each { String davidId ->
                def goTerms = davidId2Go[davidId] ?: new LinkedList<>()
                def termList = geneId2Go[geneName]
                if (!termList)
                    geneId2Go.put(geneName, termList = new LinkedList<GoTerm>())
                termList.addAll(goTerms)
            }
        }
    }

    List<GoTerm> goTerms(String geneName) {
        geneId2Go[geneName] ?: new LinkedList<>()
    }

    List<GoTerm> goTerms(Transcript transcript) {
        goTerms(transcript.name)
    }
}

/*
 * Copyright 2022-2023 Jürgen Weismüller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.weismueller.doco.entity;

import de.weismueller.doco.DocoCustomization;
import de.weismueller.doco.DocoProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DocumentComparatorTest {

    @Test
    public void testCompare() throws Exception {
        List<String> sortOrder = List.of("Result.*", "Agreement", "Letter", "Other");
        List<String> actual = List.of("Results overview", "Letter", "Other", "Agreement", "TOP 2", "TOP 1",
                "Additional information for TOP 1");
        List<String> expect = List.of("Results overview", "Agreement", "Letter", "Other", "TOP 1",
                "Additional information for TOP 1", "TOP 2");
        DocumentComparator documentComparator = new DocumentComparator(getDocoCustomization(sortOrder));
        List<Document> list = inputList(actual);
        Collections.sort(list, documentComparator);
        System.out.println(outputName(list));
        System.out.println(outputKey(list));
        Assertions.assertLinesMatch(expect, outputName(list));
    }

    private List<Document> inputList(List<String> list) {
        return list.stream().map(s -> {
            Document doc = new Document();
            doc.setName(s);
            return doc;
        }).collect(Collectors.toList());
    }

    private List<String> outputName(Collection<Document> list) {
        return list.stream().map(Document::getName).toList();
    }

    private List<String> outputKey(Collection<Document> list) {
        return list.stream().map(Document::getKey).toList();
    }

    private DocoCustomization getDocoCustomization(List<String> sortOrder) {
        DocoProperties properties = new DocoProperties();
        DocoCustomization cust = new DocoCustomization(properties);
        cust.setDocumentAgendaItemKeyword("TOP");
        cust.setDocumentSortOrder(sortOrder);
        return cust;
    }
}

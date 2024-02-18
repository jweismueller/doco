/*
 * Copyright 2022-2024 Jürgen Weismüller.
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

import de.weismueller.doco.DocoProperties;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CollectionComparator implements Comparator<Collection> {

    private final Map<String, String> collectionOrder = new TreeMap<>();

    public CollectionComparator(DocoProperties properties) {
        int n = properties.getCollectionOrder().size();
        for (int i = 0; i < n; i++) {
            collectionOrder.put(String.valueOf((char) ('a' + i)), properties.getCollectionOrder().get(i));
        }
    }

    private String key(Collection o) {
        String key = o.getDate().toString() + "_";
        key += (o.getTime() == null ? "00:00" : o.getTime().toString()) + "_";
        Set<Map.Entry<String, String>> entries = collectionOrder.entrySet();
        String colSortKey = "z";
        for (Map.Entry<String, String> entry : entries) {
            Matcher matcher = Pattern.compile(entry.getValue()).matcher(o.getTitle());
            if (matcher.matches()) {
                colSortKey = entry.getKey();
                break;
            }
        }
        key += colSortKey + "_";
        key += o.getPhysicalFolder();
        return key;
    }

    @Override
    public int compare(Collection o1, Collection o2) {
        String key1 = key(o1);
        String key2 = key(o2);
        // reverse order
        return key2.compareTo(key1);
    }
}

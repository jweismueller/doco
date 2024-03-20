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
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentComparator implements Comparator<Document> {

    private final Map<String, String> documentOrder = new TreeMap<>();

    private String documentAgendaItemKeyword = null;

    public DocumentComparator(DocoCustomization customization) {
        int n = customization.getDocumentSortOrder().size();
        for (int i = 0; i < n; i++) {
            documentOrder.put(String.valueOf((char) ('a' + i)), customization.getDocumentSortOrder().get(i));
        }
        this.documentAgendaItemKeyword = customization.getDocumentAgendaItemKeyword();
    }

    @Override
    public int compare(Document o1, Document o2) {
        String key1 = key(o1);
        String key2 = key(o2);
        return key1.compareTo(key2);
    }

    private String key(Document o) {
        String key = "";
        String normalizedName = normalize(o.getName());
        Set<Map.Entry<String, String>> entries = documentOrder.entrySet();
        String colSortKey = "y";
        // agenda items are sorted at the end
        if (containsDocumentAgendaItemKeyword(normalizedName)) {
            colSortKey = "z";
        } else {
            for (Map.Entry<String, String> entry : entries) {
                Matcher matcher = Pattern.compile(entry.getValue()).matcher(normalizedName);
                if (matcher.matches()) {
                    colSortKey = entry.getKey();
                    break;
                }
            }
        }
        key = colSortKey + "_";
        //
        if (startsWithDocumentAgendaItemKeyword(normalizedName)) {
            String paddedName = padNumbers(normalizedName);
            key += "b_" + paddedName + "_a";
        } else if (containsDocumentAgendaItemKeyword(normalizedName)) {
            String paddedName = padNumbers(normalizedName);
            paddedName = paddedName.replaceFirst(".*" + documentAgendaItemKeyword, "");
            key += "b_" + paddedName + "_b";
        } else {
            // all others are sorted before the agenda items
            key += "a_000_000_a";
        }
        key += "_" + normalizedName;
        o.setKey(key);
        return key;
    }

    private boolean startsWithDocumentAgendaItemKeyword(String s) {
        if (StringUtils.hasText(documentAgendaItemKeyword)) {
            documentAgendaItemKeyword = documentAgendaItemKeyword.toLowerCase(Locale.ROOT);
            return s.startsWith(documentAgendaItemKeyword);
        } else {
            return false;
        }
    }

    private boolean containsDocumentAgendaItemKeyword(String s) {
        if (StringUtils.hasText(documentAgendaItemKeyword)) {
            documentAgendaItemKeyword = documentAgendaItemKeyword.toLowerCase(Locale.ROOT);
            return s.contains(documentAgendaItemKeyword);
        } else {
            return false;
        }
    }

    private String normalize(String s) {
        s = s.toLowerCase(Locale.ROOT);
        s = s.replaceAll("\\s", "");
        return s;
    }

    private String padNumbers(String s) {
        String out = "";
        Pattern p = Pattern.compile("\\D?([0-9]+)\\D?([0-9]+)?.*");
        Matcher m = p.matcher(s);
        if (m.find()) {
            String f1 = m.group(1);
            String f2 = m.group(2);
            int i1 = Integer.parseInt(f1);
            out += String.format("%03d", i1);
            if (f2 != null) {
                int i2 = Integer.parseInt(f2);
                out += "_" + String.format("%03d", i2);
            } else {
                out += "_000";
            }
        }
        return out;
    }

}

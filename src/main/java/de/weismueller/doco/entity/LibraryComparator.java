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

import java.util.Comparator;

public class LibraryComparator implements Comparator<Library> {

    @Override
    public int compare(Library o1, Library o2) {
        if (o1.getId().equals(o2.getId())) {
            return 0;
        } else if (o1.getId().equals(Integer.valueOf(1))) {
            return -1;
        } else if (o2.getId().equals(Integer.valueOf(1))) {
            return 1;
        } else {
            return o2.getCreatedAt().compareTo(o1.getCreatedAt());
        }
    }

}

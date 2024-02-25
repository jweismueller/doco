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

public class UserLibraryComparator implements Comparator<UserLibrary> {

    private LibraryComparator libraryComparator = new LibraryComparator();

    @Override
    public int compare(UserLibrary o1, UserLibrary o2) {
        return libraryComparator.compare(o1.getLibrary(), o2.getLibrary());
    }

}

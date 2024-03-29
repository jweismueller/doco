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

public class UserComparator implements Comparator<User> {

    @Override
    public int compare(User o1, User o2) {
        return key(o1).compareTo(key(o2));
    }

    static String key(User o) {
        return o.getLastName() + "_" + o.getFirstName() + "_" + o.getUsername();
    }
}

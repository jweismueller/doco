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

package de.weismueller.doco;

import de.weismueller.doco.entity.Library;
import de.weismueller.doco.entity.UserLibrary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DocoUser extends User {

    private List<UserLibrary> userLibraryList = new ArrayList<>();

    public DocoUser(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
    }

    public DocoUser(String username, String password, boolean enabled, boolean accountNonExpired,
            boolean credentialsNonExpired, boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
    }

    public List<Integer> getLibraryIds() {
        return this.userLibraryList.stream().map(userLibrary -> userLibrary.getLibrary().getId()).toList();
    }

    public List<Library> getLibraries() {
        return this.userLibraryList.stream().map(userLibrary -> userLibrary.getLibrary()).toList();
    }

    public boolean hasCollectionAccess(Integer collection) {
        return getLibraries().stream()
                .anyMatch(l -> l.getCollections().stream().anyMatch(c -> c.getId().equals(collection)));
    }

    public boolean hasLibraryAccess(Integer libraryId) {
        return this.userLibraryList.stream()
                .anyMatch(userLibrary -> userLibrary.getLibrary().getId().equals(libraryId));
    }

    public void addLibrary(UserLibrary userLibrary) {
        this.userLibraryList.add(userLibrary);
    }
}

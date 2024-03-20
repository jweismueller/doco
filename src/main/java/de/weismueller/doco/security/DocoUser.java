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

package de.weismueller.doco.security;

import de.weismueller.doco.entity.Library;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DocoUser extends User {

    private de.weismueller.doco.entity.User user;

    public DocoUser(de.weismueller.doco.entity.User user, List<GrantedAuthority> authorities) {
        this(user.getUsername(), user.getPassword(), authorities);
        this.user = user;
    }

    public DocoUser(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
    }

    public DocoUser(String username, String password, boolean enabled, boolean accountNonExpired,
            boolean credentialsNonExpired, boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
    }

    public void updateUser(de.weismueller.doco.entity.User user) {
        this.user = user;
    }

    public List<Integer> getLibraryIds() {
        return this.user.getLibraries().stream().map(l -> l.getId()).toList();
    }

    public Set<Library> getLibraries() {
        return this.user.getLibraries();
    }

    public boolean hasCollectionAccess(Integer collectionId) {
        return getLibraries().stream()
                .anyMatch(l -> l.getCollections().stream().anyMatch(c -> c.getId().equals(collectionId)));
    }

    public boolean hasLibraryAccess(Integer libraryId) {
        return this.user.getLibraries().contains(libraryId);
    }
}

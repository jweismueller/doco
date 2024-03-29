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

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Entity
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(length = 64)
    private String username;
    @Column(length = 128)
    private String password;
    @Column
    private boolean enabled;
    @Column(length = 64)
    private String firstName;
    @Column(length = 64)
    private String lastName;
    @Enumerated(EnumType.STRING)
    private UserTitleType title;
    @Column(length = 64)
    private String userGroup;
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Library> libraries;

}

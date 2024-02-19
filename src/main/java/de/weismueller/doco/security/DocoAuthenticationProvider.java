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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocoAuthenticationProvider extends DaoAuthenticationProvider {

    @Autowired
    private void setDocoUserDetailsService(DocoUserDetailsService docoUserDetailsService) {
        super.setUserDetailsService(docoUserDetailsService);
    }

    @Autowired
    private void setDocoPasswordEncoder(PasswordEncoder passwordEncoder) {
        super.setPasswordEncoder(passwordEncoder);
    }

    @Override
    protected Authentication createSuccessAuthentication(Object principal, Authentication authentication,
            UserDetails user) {
        Authentication successAuthentication = super.createSuccessAuthentication(principal, authentication, user);
        return successAuthentication;
    }
}

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

package de.weismueller.doco;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;


@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class DocoControllerAdvice {

    private final DocoCustomization customization;
    private final DocoProperties properties;

    public static boolean _isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    @ModelAttribute("currentUser")
    public DocoUser getCurrentUser(Authentication authentication) {
        return (authentication == null) ? null : (DocoUser) authentication.getPrincipal();
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        return DocoControllerAdvice._isAdmin(authentication);
    }

    @ModelAttribute
    public void addCustomization(Model model) {
        model.addAttribute("customization", customization);
    }

}

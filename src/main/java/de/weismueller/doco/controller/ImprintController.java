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

package de.weismueller.doco.controller;

import de.weismueller.doco.DocoCustomization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ImprintController {

    private final DocoCustomization customization;

    @GetMapping("/imprint")
    public String imprint(Model model, Authentication authentication) {
        String imprintContent = customization.getImprintContent();
        model.addAttribute("imprintContent", imprintContent);
        return "imprint";
    }

}

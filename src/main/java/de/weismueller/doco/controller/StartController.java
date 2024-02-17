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

import com.google.common.io.BaseEncoding;
import de.weismueller.doco.DocoControllerAdvice;
import de.weismueller.doco.DocoCustomization;
import de.weismueller.doco.DocoProperties;
import de.weismueller.doco.DocoUser;
import de.weismueller.doco.entity.Collection;
import de.weismueller.doco.entity.CollectionComparator;
import de.weismueller.doco.entity.CollectionRepository;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.TreeSet;

@Controller
@Slf4j
@RequiredArgsConstructor
public class StartController {

    private final CollectionRepository collectionRepository;
    private final DocoProperties properties;
    private final DocoCustomization customization;
    private final ServletContext servletContext;

    @GetMapping(value = "/favicon.ico")
    public @ResponseBody byte[] getFavicon() throws Exception {
        return BaseEncoding.base64().decode(customization.getFaviconBase64());
    }

    @GetMapping(value = "/logo.png")
    public @ResponseBody byte[] getLogo() throws Exception {
        return BaseEncoding.base64().decode(customization.getLogoBase64());
    }

    @GetMapping(value = {"", "/", "/start"})
    public String start(Model model, Authentication authentication) {
        DocoUser user = (DocoUser) authentication.getPrincipal();
        Iterable<Collection> all = collectionRepository.findByLibraryIdIn(user.getLibraryIds());
        TreeSet<Collection> sorted = new TreeSet<>(new CollectionComparator(properties));
        boolean isAdmin = DocoControllerAdvice._isAdmin(authentication);
        for (Collection collection : all) {
            if (!collection.getEnabled()) {
                collection.setCssClass("text-black-25");
            }
            if (collection.getEnabled() || isAdmin) {
                sorted.add(collection);
            }
        }
        //all.forEach(a -> sorted.add(a));
        model.addAttribute("collections", sorted);
        return "start";
    }

}

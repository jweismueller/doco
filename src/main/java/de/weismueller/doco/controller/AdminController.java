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

package de.weismueller.doco.controller;

import de.weismueller.doco.DocoProperties;
import de.weismueller.doco.DocoUser;
import de.weismueller.doco.entity.*;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminController {

    private final CollectionRepository collectionRepository;
    private final LibraryRepository libraryRepository;
    private final UserRepository userRepository;
    private final DocoProperties properties;
    private final CollectionController collectionController;
    private final ServletContext servletContext;
    private final PasswordEncoder encoder;

    @GetMapping("/admin/user/{id}")
    public String getAdminUser(@PathVariable("id") Integer id, Model model) {
        Optional<User> byId = userRepository.findById(id);
        model.addAttribute("user", byId.get());
        return "admin/user";
    }

    @PostMapping("/admin/user/{id}/resetPassword")
    public String postAdminResetPassword(@PathVariable("id") Integer id, Model model) {
        Optional<User> byId = userRepository.findById(id);
        User user = byId.get();
        String newPassword = generatePassword();
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
        model.addAttribute("newPassword", newPassword);
        model.addAttribute("user", byId.get());
        return "admin/user";
    }

    @GetMapping("/admin/user")
    public String getAdminUsers(Model model) {
        List<User> users = new ArrayList<>();
        userRepository.findAll().iterator().forEachRemaining(users::add);
        Collections.sort(users, new UserComparator());
        model.addAttribute("users", users);
        return "admin/user";
    }

    @GetMapping("/admin/collection")
    public String getAdminCollection(Model model, Authentication authentication) {
        DocoUser user = (DocoUser) authentication.getPrincipal();
        model.addAttribute("collection", new Collection());
        model.addAttribute("libraries", user.getLibraries());
        return "admin/collection";
    }

    @GetMapping("/admin/library/{libraryId}/collection/{collectionId}")
    public String getCollection(@PathVariable("libraryId") Integer libraryId,
            @PathVariable("collectionId") Integer collectionId, Model model) {
        Optional<Collection> collection = collectionController.loadCollection(libraryId, collectionId);
        if (collection.isEmpty()) {
            log.error("Collection with id {} not available.", collectionId);
        }
        model.addAttribute("collection", collection.get());
        return "admin/collection";
    }

    @PostMapping("/admin/library/{libraryId}/collection/{collectionId}/document/{documentId}/delete")
    public String deleteDocument(@PathVariable("libraryId") Integer libraryId,
            @PathVariable("collectionId") Integer collectionId, @PathVariable("documentId") String documentId,
            Model model) {
        Optional<Collection> collection = collectionController.loadCollection(libraryId, collectionId);
        if (collection.isEmpty()) {
            log.error("Collection with id {} not available.", collectionId);
        }
        Optional<Document> first = collection.get()
                .getDocuments()
                .stream()
                .filter(d -> d.getId().equals(documentId))
                .findAny();
        if (first.isEmpty()) {
            log.error("Document inside collection {} with id {} not available.", collectionId, documentId);
        }
        Path pathFile = Path.of(properties.getPathDocuments(), collection.get().getPhysicalFolder(),
                first.get().getName());
        try {
            Files.delete(pathFile);
        } catch (Exception e) {
            log.error("Error deleting file " + pathFile + " from disk: " + e.toString());
        }
        String redirect = String.format("redirect:/library/%d/collection/%d", libraryId, collectionId);
        return redirect;
    }

    @PostMapping(value = "/admin/collection", consumes = "application/x-www-form-urlencoded")
    public String postCollection(Model model, Authentication authentication, @RequestBody MultiValueMap body) {
        String collectionDate = String.valueOf(body.getFirst("collectionDate"));
        String collectionTime = String.valueOf(body.getFirst("collectionTime"));
        String collectionTitle = String.valueOf(body.getFirst("collectionTitle"));
        String collectionId = String.valueOf(body.getFirst("collectionId"));
        String collectionLibraryId = String.valueOf(body.getFirst("collectionLibraryId"));
        boolean collectionEnabled = "on".equals(String.valueOf(body.getFirst("collectionEnabled")));
        //
        Library library = libraryRepository.findById(Integer.parseInt(collectionLibraryId)).get();
        //
        Collection collection = null;
        if (StringUtils.hasText(collectionId)) {
            Optional<Collection> byId = collectionRepository.findById(Integer.parseInt(collectionId));
            collection = byId.get();
            collection.setEnabled(collectionEnabled);
        } else {
            collection = new Collection();
            collection.setCreatedAt(LocalDateTime.now());
            collection.setCreatedBy(authentication.getName());
            collection.setEnabled(true);
            String digestAsHex = DigestUtils.md5DigestAsHex(collectionTitle.getBytes(StandardCharsets.UTF_8))
                    .substring(0, 8);
            collection.setPhysicalFolder(digestAsHex);
        }
        collection.setDate(LocalDate.parse(collectionDate));
        collection.setLibrary(library);
        if (StringUtils.hasText(collectionTime)) {
            collection.setTime(LocalTime.parse(collectionTime));
        } else {
            collection.setTime(null);
        }
        collection.setModifiedAt(LocalDateTime.now());
        collection.setModifiedBy(authentication.getName());
        collection.setTitle(collectionTitle);
        //
        collectionRepository.save(collection);
        //
        return "redirect:/";
    }

    @PostMapping(value = "/admin/library/{libraryId}/collection/{collectionId}", consumes = "multipart/form-data")
    public ResponseEntity<Void> uploadFile(@PathVariable("libraryId") Integer libraryId,
            @PathVariable("collectionId") Integer collectionId,
            @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            log.error("File is empty.");
            return ResponseEntity.notFound().build();
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        log.info("Got file upload: " + fileName);
        Optional<Collection> collection = collectionController.loadCollection(libraryId, collectionId);
        if (collection.isEmpty()) {
            log.error("Collection with id {} not available.", collectionId);
            return ResponseEntity.notFound().build();
        }
        Path pathDir = Path.of(properties.getPathDocuments(), collection.get().getPhysicalFolder());
        if (!pathDir.toFile().exists()) {
            pathDir.toFile().mkdir();
        }
        Path pathFile = Path.of(properties.getPathDocuments(), collection.get().getPhysicalFolder(), fileName);
        try {
            Files.copy(file.getInputStream(), pathFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("Error saving file to disk: " + e.toString());
        }
        return ResponseEntity.created(URI.create("/")).build();
    }

    public String generatePassword() {
        String upperCaseLetters = RandomStringUtils.random(4, 65, 90, true, true);
        String lowerCaseLetters = RandomStringUtils.random(4, 97, 122, true, true);
        String numbers = RandomStringUtils.randomNumeric(2);
        String combinedChars = upperCaseLetters.concat(lowerCaseLetters).concat(numbers);
        List<Character> pwdChars = combinedChars.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        Collections.shuffle(pwdChars);
        String password = pwdChars.stream()
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        return password;
    }

}

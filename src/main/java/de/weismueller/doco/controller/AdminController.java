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

import com.google.common.collect.Streams;
import de.weismueller.doco.DocoProperties;
import de.weismueller.doco.entity.*;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping("/admin")
    public String getAdmin(Model model) {
        return "admin/admin";
    }

    @GetMapping("/admin/library")
    public String getAdminLibraryList(Model model, HttpServletRequest request, Authentication authentication) {
        boolean isNew = Streams.stream(request.getParameterNames().asIterator()).anyMatch("new"::equals);
        if (isNew) {
            model.addAttribute("library", new Library());
        } else {
            List<Library> libraries = new ArrayList<>();
            libraryRepository.findAll().iterator().forEachRemaining(libraries::add);
            Collections.sort(libraries, new LibraryComparator());
            model.addAttribute("libraries", libraries);
        }
        return "admin/library";
    }

    @GetMapping("/admin/library/{id}")
    public String getAdminLibraryById(@PathVariable("id") Integer id, Model model) {
        libraryRepository.findById(id).ifPresent(l -> model.addAttribute("library", l));
        final List<Collection> collections = new ArrayList<>();
        collectionRepository.findAll().iterator().forEachRemaining(c -> {
            if (!c.getEnabled()) {
                c.setTransientCssClass("text-black-25");
            }
            collections.add(c);
        });
        Collections.sort(collections, new CollectionComparator(properties));
        model.addAttribute("collections", collections);
        model.addAttribute("selectedCollections", libraryRepository.findById(id)
                .get()
                .getCollections()
                .stream()
                .map(c -> c.getId())
                .collect(Collectors.toSet()));
        return "admin/library";
    }

    @GetMapping("/admin/user")
    public String getAdminUserList(Model model, HttpServletRequest request, Authentication authentication) {
        boolean isNew = Streams.stream(request.getParameterNames().asIterator()).anyMatch("new"::equals);
        if (isNew) {
            model.addAttribute("user", new User());
        } else {
            List<User> users = new ArrayList<>();
            userRepository.findAll().iterator().forEachRemaining(users::add);
            Collections.sort(users, new UserComparator());
            model.addAttribute("users", users);
            model.addAttribute("libraries", new ArrayList<>());
        }
        return "admin/user";
    }

    @GetMapping("/admin/user/{id}")
    public String getAdminUserById(@PathVariable("id") Integer id, Model model) {
        Optional<User> byId = userRepository.findById(id);
        model.addAttribute("user", byId.get());
        List<Library> libraries = libraryRepository.findAll();
        Collections.sort(libraries, new LibraryComparator());
        model.addAttribute("libraries", libraries);
        model.addAttribute("selectedLibraries", libraries.stream()
                .filter(l -> byId.get().getLibraries().contains(l))
                .map(l -> l.getId())
                .collect(Collectors.toList()));
        return "admin/user";
    }

    @PostMapping("/admin/user/{id}/resetPassword")
    public String postAdminUserResetPassword(@PathVariable("id") Integer id, Model model) {
        Optional<User> byId = userRepository.findById(id);
        User user = byId.get();
        String newPassword = generatePassword();
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
        model.addAttribute("newPassword", newPassword);
        model.addAttribute("user", byId.get());
        return "admin/user";
    }

    @PostMapping("/admin/user")
    public String postAdminUser(Model model, @RequestBody MultiValueMap body) {
        String firstName = String.valueOf(body.getFirst("firstName"));
        String lastName = String.valueOf(body.getFirst("lastName"));
        String title = String.valueOf(body.getFirst("userTitle"));
        String userId = String.valueOf(body.getFirst("userId"));
        String username = String.valueOf(body.getFirst("username"));
        boolean userEnabled = "on".equals(String.valueOf(body.getFirst("userEnabled")));
        User user;
        if (StringUtils.hasText(userId)) {
            user = userRepository.findById(Integer.parseInt(userId)).get();
            user.getLibraries().clear();
            body.keySet()
                    .stream()
                    .filter(k -> (String.valueOf(k).startsWith("lib")))
                    .map(k -> String.valueOf(k).substring(3))
                    .forEach(k -> {
                        Optional<Library> byId = libraryRepository.findById(Integer.parseInt("" + k));
                        byId.ifPresent(c -> user.getLibraries().add(c));
                    });
            user.setEnabled(userEnabled);
        } else {
            user = new User();
            user.setEnabled(true);
            user.setPassword(encoder.encode(generatePassword()));
            user.setUsername(username);
        }
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setTitle(UserTitleType.valueOf(title));
        User saved = userRepository.save(user);
        String redirect = String.format("redirect:/admin/user/%d", saved.getId());
        return redirect;
    }

    @GetMapping("/admin/collection")
    public String getAdminCollectionList(Model model, HttpServletRequest request, Authentication authentication) {
        boolean isNew = Streams.stream(request.getParameterNames().asIterator()).anyMatch("new"::equals);
        model.addAttribute("libraries", libraryRepository.findAll());
        if (isNew) {
            model.addAttribute("collection", new Collection());
        } else {
            model.addAttribute("collections", collectionRepository.findAll());
        }
        return "admin/collection";
    }

    @GetMapping("/admin/collection/{collectionId}")
    public String getAdminCollectionById(@PathVariable("collectionId") Integer collectionId, Model model) {
        Optional<Collection> collection = collectionController.loadCollection(collectionId);
        model.addAttribute("collection", collection.get());
        return "admin/collection";
    }

    @PostMapping("/admin/collection/{collectionId}/document/{documentId}/delete")
    public String deleteAdminDocument(@PathVariable("collectionId") Integer collectionId,
            @PathVariable("documentId") String documentId, Model model) {
        Optional<Collection> collection = collectionController.loadCollection(collectionId);
        if (collection.isEmpty()) {
            log.error("Collection with id {} not available.", collectionId);
        }
        Optional<Document> first = collection.get()
                .getTransientDocuments()
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
        String redirect = String.format("redirect:/collection/%d", collectionId);
        return redirect;
    }

    @PostMapping(value = "/admin/collection", consumes = "application/x-www-form-urlencoded")
    public String postAdminCollection(Model model, Authentication authentication, @RequestBody MultiValueMap body) {
        String collectionDate = String.valueOf(body.getFirst("collectionDate"));
        String collectionTime = String.valueOf(body.getFirst("collectionTime"));
        String collectionTitle = String.valueOf(body.getFirst("collectionTitle"));
        String collectionId = String.valueOf(body.getFirst("collectionId"));
        boolean collectionEnabled = "on".equals(String.valueOf(body.getFirst("collectionEnabled")));
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
        return "redirect:/admin/collection";
    }

    @PostMapping(value = "/admin/collection/{collectionId}", consumes = "multipart/form-data")
    public ResponseEntity<Void> postAdminUploadFile(@PathVariable("collectionId") Integer collectionId,
            @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            log.error("File is empty.");
            return ResponseEntity.notFound().build();
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        log.info("Got file upload: " + fileName);
        Optional<Collection> collection = collectionController.loadCollection(collectionId);
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

    @PostMapping(value = "/admin/library", consumes = "application/x-www-form-urlencoded")
    public String postAdminLibrary(Model model, Authentication authentication, @RequestBody MultiValueMap body) {
        String libraryId = String.valueOf(body.getFirst("libraryId"));
        String libraryTitle = String.valueOf(body.getFirst("libraryTitle"));
        //
        if (StringUtils.hasText(libraryId)) {
            Optional<Library> byId = libraryRepository.findById(Integer.parseInt(libraryId));
            final Library library = byId.get();
            library.setTitle(libraryTitle);
            library.getCollections().clear();
            body.keySet()
                    .stream()
                    .filter(k -> (String.valueOf(k).startsWith("col")))
                    .map(k -> String.valueOf(k).substring(3))
                    .forEach(k -> {
                        Optional<Collection> byId1 = collectionRepository.findById(Integer.parseInt("" + k));
                        byId1.ifPresent(c -> library.getCollections().add(c));
                    });
            library.setModifiedAt(LocalDateTime.now());
            library.setModifiedBy(authentication.getName());
            libraryRepository.save(library);
        } else {
            final Library library = new Library();
            library.setEnabled(true);
            library.setCreatedAt(LocalDateTime.now());
            library.setCreatedBy(authentication.getName());
            library.setTitle(libraryTitle);
            library.setModifiedAt(LocalDateTime.now());
            library.setModifiedBy(authentication.getName());
            libraryRepository.save(library);
        }
        return "redirect:/admin/library";
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

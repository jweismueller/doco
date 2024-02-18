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

import de.weismueller.doco.DocoProperties;
import de.weismueller.doco.DocoUser;
import de.weismueller.doco.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequiredArgsConstructor
public class CollectionController {

    private final LibraryRepository libraryRepository;
    private final CollectionRepository collectionRepository;
    private final DocoProperties properties;

    @GetMapping("/library/{libraryId}/collection/{collectionId}")
    public String collection(@PathVariable("libraryId") Integer libraryId, @PathVariable("collectionId") Integer collectionId, Model model) {
        DocoUser user = (DocoUser) model.getAttribute("currentUser");
        if (!user.hasLibraryAccess(libraryId)) {
            log.error("User {} has no access to library {}", user.getUsername(), libraryId);
            return "redirect:/";
        }
        Optional<Library> library = libraryRepository.findById(libraryId);
        Optional<Collection> collection = loadCollection(libraryId, collectionId);
        if (collection.isPresent()) {
            model.addAttribute("collection", collection.get());
        }
        return "collection";
    }

    @GetMapping("/library/{libraryId}/collection/{collectionId}/document/{documentId}")
    public String fileRedirect(@PathVariable("libraryId") Integer libraryId, @PathVariable("collectionId") Integer collectionId, @PathVariable("documentId") String documentId, Model model) {
        Optional<Document> document = loadDocument(libraryId, collectionId, documentId);
        if (document.isEmpty()) {
            return "redirect:/";
        }
        String encodedTitle = URLEncoder.encode(document.get().getName(), StandardCharsets.UTF_8);
        String redirect = String.format("redirect:/library/%d/collection/%d/document/%s/%s", libraryId, collectionId, documentId, encodedTitle);
        return redirect;
    }

    @GetMapping("/library/{libraryId}/collection/{collectionId}/document/{documentId}/{name}")
    public ResponseEntity<Resource> getDocument(@PathVariable("libraryId") Integer libraryId, @PathVariable("collectionId") Integer collectionId, @PathVariable("documentId") String documentId, @PathVariable("name") String name, Model model) {
        Optional<Document> document = loadDocument(libraryId, collectionId, documentId);
        if (document.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        //headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + document.getName());
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + document.get().getName());
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");
        ByteArrayResource resource = null;
        try {
            resource = new ByteArrayResource(Files.readAllBytes(document.get().getPath()));
        } catch (IOException e) {
            log.error("Error accessing collection {} with document {}: " + e.toString(), collectionId, documentId);
            return ResponseEntity.notFound().build();
        }
        long fileLength = document.get().getPath().toFile().length();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().headers(headers).contentLength(fileLength);
        if ("download".equals(name)) {
            builder.contentType(MediaType.APPLICATION_OCTET_STREAM);
        } else {
            builder.contentType(MediaType.APPLICATION_PDF);
        }
        return builder.body(resource);
    }

    private Optional<Document> loadDocument(Integer libraryId, Integer collectionId, String documentId) {
        Optional<Collection> collection = loadCollection(libraryId, collectionId);
        Document document = null;
        if (collection.isPresent() && documentId != null) {
            return collection.get().getDocuments().stream().filter(e -> e.getId().equals(documentId)).findAny();
        }
        return Optional.empty();
    }

    public Optional<Collection> loadCollection(Integer libraryId, Integer collectionId) {
        Optional<Collection> collection = collectionRepository.findById(collectionId);
        if (!collection.isPresent()) {
            return collection;
        }
        if (!collection.get().getLibrary().getId().equals(libraryId)) {
            return Optional.empty();
        }
        try {
            loadDocuments(collection.get());
        } catch (Exception e) {
            log.error("Error accessing collection with id: " + collectionId);
            log.error(e.toString()); // no stack trace necessary
        }
        return collection;
    }

    private void loadDocuments(Collection collection) throws Exception {
        collection.setDocuments(new TreeSet<>(new DocumentComparator(properties)));
        Path p = Path.of(properties.getPathDocuments(), collection.getPhysicalFolder());
        List<Path> paths = Files.list(p).collect(Collectors.toList());
        int i = 0;
        for (Path path : paths) {
            Document doc = new Document();
            String name = path.getFileName().toString();
            String digestAsHex = DigestUtils.md5DigestAsHex(name.getBytes(StandardCharsets.UTF_8)).substring(0, 8);
            doc.setId(digestAsHex);
            doc.setName(name);
            doc.setPath(path);
            collection.getDocuments().add(doc);
        }
    }

}

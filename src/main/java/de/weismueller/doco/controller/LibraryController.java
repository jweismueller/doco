package de.weismueller.doco.controller;

import de.weismueller.doco.DocoProperties;
import de.weismueller.doco.entity.CollectionRepository;
import de.weismueller.doco.entity.Library;
import de.weismueller.doco.entity.LibraryRepository;
import de.weismueller.doco.entity.UserLibraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
@Slf4j
@RequiredArgsConstructor
public class LibraryController {

    private final CollectionRepository collectionRepository;
    private final DocoProperties properties;
    private final LibraryRepository libraryRepository;
    private final UserLibraryRepository userLibraryRepository;

    @GetMapping("/library/{id}")
    public String collection(@PathVariable("id") Integer id, Model model) {
        Optional<Library> collection = libraryRepository.findById(id);
        if (collection.isPresent()) {
            model.addAttribute("library", collection.get());
        }
        return "library";
    }



}

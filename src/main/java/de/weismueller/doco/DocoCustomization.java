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

package de.weismueller.doco;

import com.google.common.base.CharMatcher;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.xml.DocumentBuilderFactoryUtils;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Data
@Service
@Scope("singleton")
@Slf4j
@RequiredArgsConstructor
public class DocoCustomization {

    private final DocoProperties properties;
    private String environment;
    private String javaScript;
    private String helpOrder;
    private String brandColor;
    private String imprintContent;
    private String imprintLink;
    private String faviconBase64;
    private String logoBase64;
    private List<String> documentSortOrder;
    private String documentAgendaItemKeyword;

    private static String getContent(Document document, Document defaultDocument, String tag) {
        if (document != null) {
            String out = document.getElementsByTagName(tag).item(0).getTextContent();
            if (StringUtils.hasText(out)) {
                log.info("Using customization for {} from environment: {}", tag,
                        document.getDocumentElement().getAttribute("environment"));
                return out;
            }
        }
        log.info("Using customization for {} from environment: {}", tag,
                defaultDocument.getDocumentElement().getAttribute("environment"));
        return defaultDocument.getElementsByTagName(tag).item(0).getTextContent();
    }

    @PostConstruct
    public void init() throws Exception {
        //
        InputStream is = this.getClass().getResourceAsStream("/doco.xml");
        DocumentBuilderFactory dbf = DocumentBuilderFactoryUtils.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        Document defaultDocument = dbf.newDocumentBuilder().parse(is);
        String file = properties.getCustomizationFile();
        Document document = null;
        if (StringUtils.hasText(file)) {
            File f = Path.of(file).toFile();
            if (f.exists()) {
                log.info("Loading customization from file: " + f.getAbsolutePath());
                document = dbf.newDocumentBuilder().parse(f);
            }
        }
        //
        helpOrder = getContent(document, defaultDocument, "help-order");
        brandColor = getContent(document, defaultDocument, "brand-color");
        imprintContent = getContent(document, defaultDocument, "imprint-content");
        imprintLink = getContent(document, defaultDocument, "imprint-link");
        faviconBase64 = getContent(document, defaultDocument, "favicon-base64");
        faviconBase64 = CharMatcher.whitespace().removeFrom(faviconBase64);
        logoBase64 = getContent(document, defaultDocument, "logo-base64");
        logoBase64 = CharMatcher.whitespace().removeFrom(logoBase64);
        javaScript = getContent(document, defaultDocument, "java-script");
        documentAgendaItemKeyword = getContent(document, defaultDocument, "document-agenda-item-keyword");
        documentSortOrder = List.of(getContent(document, defaultDocument, "document-sort-order").split(","));
        //
    }

    public boolean isImprintLinkFilled() {
        return StringUtils.hasText(imprintLink);
    }

    public List<String> getDocumentSortOrder() {
        if (documentSortOrder == null) {
            return Collections.emptyList();
        } else {
            return documentSortOrder;
        }
    }
}

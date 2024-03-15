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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class DocoConfiguration implements WebMvcConfigurer {

    private final DocoProperties properties;
    private final DocoCustomization customization;

    public void addInterceptors(InterceptorRegistry registry) {
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        registry.addInterceptor(loggingInterceptor);
    }

    @Bean("messageSource")
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding("UTF-8");
        MyMessageSource myMessageSource = new MyMessageSource(messageSource, customization);
        return myMessageSource;
    }

    @Slf4j
    @RequiredArgsConstructor
    static class MyMessageSource implements MessageSource {

        private final MessageSource delegate;
        private final DocoCustomization customization;

        @Override
        public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
            return delegate.getMessage(code, args, defaultMessage, locale);
        }

        @Override
        public String getMessage(final String code, Object[] args, Locale locale) throws NoSuchMessageException {
            String s = customization.getMessageOverridesMap().get(code);
            if (StringUtils.hasText(s)) {
                return s;
            }
            return delegate.getMessage(code, args, locale);
        }

        @Override
        public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
            return delegate.getMessage(resolvable, locale);
        }
    }

}

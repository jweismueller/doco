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

import de.weismueller.doco.entity.User;
import de.weismueller.doco.entity.UserRepository;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final ServletContext servletContext;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @GetMapping("/user")
    public String user(Model model) {
        return "user";
    }

    @PostMapping("/user/changepassword")
    public RedirectView userChangePassword(Model model, HttpServletRequest request, HttpServletResponse response, Authentication authentication, @RequestBody MultiValueMap body) {
        String userNewPassword = String.valueOf(body.getFirst("userNewPassword"));
        String userNewPasswordRepeat = String.valueOf(body.getFirst("userNewPasswordRepeat"));
        RedirectView redirectView = new RedirectView();
        redirectView.setContextRelative(true);
        if (!userNewPassword.equals(userNewPasswordRepeat)) {
            redirectView.setUrl("/user?notSame");
            return redirectView;
        } else if (!complexitySufficient(userNewPassword)) {
            redirectView.setUrl("/user?notComplex");
            return redirectView;
        } else {
            // update password
            Optional<User> byUsername = userRepository.findByUsername(authentication.getName());
            if (byUsername.isEmpty()) {
                throw new RuntimeException("No user with username found in database: " + authentication.getName());
            }
            User user = byUsername.get();
            user.setPassword(encoder.encode(userNewPassword));
            userRepository.save(user);
            // logout
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                new SecurityContextLogoutHandler().logout(request, response, auth);
            }
            SecurityContextHolder.getContext().setAuthentication(null);
            // redirect to login
            redirectView.setUrl("/login?changePassword");
            return redirectView;
        }
    }

    private boolean complexitySufficient(String password) {
        return complexitySufficientUpperLowerFigures(password);
    }

    private boolean complexitySufficientWithSpecialChars(String password) {
        if (!StringUtils.hasText(password)) {
            return false;
        }
        if (StringUtils.containsWhitespace(password)) {
            return false;
        }
        if (password.length() < 8) {
            return false;
        }
        Pattern special = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]");
        Matcher hasSpecial = special.matcher(password);
        if (!hasSpecial.find()) {
            return false;
        }
        return true;
    }

    private boolean complexitySufficientUpperLowerFigures(String password) {
        if (!StringUtils.hasText(password)) {
            return false;
        }
        if (StringUtils.containsWhitespace(password)) {
            return false;
        }
        if (password.length() < 8) {
            return false;
        }
        Pattern letter1 = Pattern.compile("[a-z]");
        Pattern letter2 = Pattern.compile("[A-Z]");
        Pattern digit = Pattern.compile("[0-9]");
        Matcher hasLetter1 = letter1.matcher(password);
        Matcher hasLetter2 = letter2.matcher(password);
        Matcher hasDigit = digit.matcher(password);
        boolean ok = (hasLetter1.find() && hasLetter2.find() && hasDigit.find());
        if (!ok) {
            return false;
        }
        return true;
    }

}



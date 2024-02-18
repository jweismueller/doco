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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@Slf4j
public class DocoSecurity {

    private DataSource dataSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests((auth) -> auth

                .requestMatchers("/js/**", "/css/**", "/images/**")
                .permitAll()
                .requestMatchers("/login", "/imprint")
                .permitAll()
                .anyRequest()
                .authenticated()

        );

        http.csrf(c -> c.ignoringRequestMatchers("/admin/**"));
        http.logout(logout -> logout.logoutUrl("/logout").permitAll());
        http.formLogin(formLogin -> formLogin.loginPage("/login").permitAll());

        return http.build();
    }

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, PasswordEncoder encoder, DocoUserDetailsService userDetailsService) throws Exception {
        auth.jdbcAuthentication()
                .dataSource(dataSource)
                .passwordEncoder(encoder)
                .usersByUsernameQuery("SELECT username, password, enabled from user where username = ?")
                .authoritiesByUsernameQuery(
                        "SELECT u.username, a.authority " + "FROM user_authority a, users u " + "WHERE u.username = ? " + "AND u.id = a.user_id");
        auth.userDetailsService(userDetailsService);
    }

    /*

    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication()
                .dataSource(dataSource)
                .passwordEncoder(passwordEncoder())
                .usersByUsernameQuery("SELECT username, password, enabled from users where username = ?")
                .authoritiesByUsernameQuery(
                        "SELECT u.username, a.authority " + "FROM user_authorities a, users u " + "WHERE u.username = ? " + "AND u.id = a.user_id");
    }

    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/js/**", "/css/**", "/images/**")
                .permitAll()
                // this is necessary to forward the ?changePassword parameter to the login page
                .antMatchers("/login")
                .permitAll()
                .antMatchers("/imprint")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
                .logout()
                .logoutUrl("/logout")
                .permitAll()
                .and()
                .csrf()
                .ignoringAntMatchers("/admin/**");
    }
    */

}

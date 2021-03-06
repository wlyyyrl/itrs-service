package org.wlyyy.itrs.spring;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.wlyyy.common.utils.StringTemplateUtils.St;
import org.wlyyy.itrs.domain.UserAgent;
import org.wlyyy.itrs.service.AuthenticationServiceImpl;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableRedisHttpSession
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    private Environment env;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/myProfile/user/**").hasRole("ADMIN")
                .antMatchers("/myProfile/mydemandFollowing/**").hasAnyRole("MANAGER")
                .antMatchers("/myProfile/mydemand/**").hasAnyRole("HR", "MANAGER")
                .antMatchers("/myProfile/flow/listHistoricFlow").permitAll()
                .antMatchers("/myProfile/flow/deploy/**").hasAnyRole("ADMIN")
                .antMatchers("/myProfile/flow/**").hasAnyRole("HR", "MANAGER", "INTERVIEWEE")
                .antMatchers("/myProfile/**").permitAll()
                .and()
                .authorizeRequests()
                .anyRequest().permitAll()
                .and().cors()
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .and()
                    .formLogin()
                    .loginPage("/auth/login")
                    .successHandler((request, response, authentication) -> {
                        // @{link AuthenticationServiceImpl}
                        final UserAgent userAgent = (UserAgent) authentication.getDetails();
                        final Map<String, Object> returnMap = new HashMap<>();

                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().print(new Gson().toJson(userAgent));
                        response.setStatus(200);
                        response.setHeader("Content-Type", "application/json;charset=UTF-8");
                    })
                    .failureHandler((request, response, exception) -> {
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().print("{ \"status\": 200, \"message\": \"Login failed\"}");
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        LOG.info("Login failed with exception {}", exception.getMessage());
                    })
//                .and()
//                    .rememberMe()
//                    .rememberMeCookieName("remember-me")
//                    .userDetailsService(beanFactory.getBean(UserDetailsService.class))
//                    .tokenValiditySeconds(24 * 60 * 60) // expired time = 1 day
//                    // .tokenRepository(persistentTokenRepository())
                .and().logout().logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout")).logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                .and().csrf().disable()

        // .addFilterAfter(new CsrfGrantingFilter(), SessionManagementFilter.class)
        ;
        http.exceptionHandling().authenticationEntryPoint((request, response, authException) -> {
            if (authException != null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().print("Unauthorizated");
            }
        });
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        final String domains = env.getProperty("web.crossorigin.domains");

        if (StringUtils.isNotEmpty(domains)) {
            LOG.info("Read web.crossorigin.domains [{}]", domains);
            final String[] split = domains.split(",");
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(Arrays.stream(split).map(String::trim).collect(Collectors.toList()));
            configuration.setAllowCredentials(true);
            configuration.setAllowedMethods(Arrays.asList("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS"));
            configuration.setAllowedHeaders(Arrays.asList("x-requested-with", "content-type"));
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;

        } else {
            LOG.info("Empty web.crossorigin.domains, no CORS configured.");
            return new UrlBasedCorsConfigurationSource();
        }

    }

    /**
     * 这东西是校验入口，继承了AbstractAuthenticationProcessingFilter。
     * 它会调用{@link AuthenticationManager#authenticate} 来进行验证。
     * 一般情况下AuthenticationManager 由{@link ProviderManager}来提供。
     * ProviderManager会找到{@link AuthenticationProvider} 来进行真正的身份验证。
     * <p>
     * 我们的{@link AuthenticationServiceImpl} 实现了AuthenticationProvider，可以验证。
     *
     * @param manager 密码管理服务，boot默认生成的
     * @return UsernamePasswordAuthenticationFilter
     */
    @Bean
    public UsernamePasswordAuthenticationFilter getUsernamePasswordAuthenticationFilter(AuthenticationManager manager) {
        final UsernamePasswordAuthenticationFilter filter = new UsernamePasswordAuthenticationFilter();
        filter.setAuthenticationManager(manager);
        filter.setFilterProcessesUrl("/auth/login");
        filter.setAuthenticationSuccessHandler((request, response, authentication) -> {
            final UserAgent userAgent = (UserAgent) authentication.getPrincipal();
            final String sessionKey = userAgent.getSessionKey();
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print(St.r("{ \"status\": 200, \"sessionKey\": \"{}\", \"userName\": \"{}\"" +
                            ", \"realName\": \"{}\", \"sex\": \"{}\" }",
                    sessionKey,
                    userAgent.getUserName(),
                    userAgent.getRealName(),
                    userAgent.getSex()));
            response.setStatus(200);
            response.setHeader("Content-Type", "application/json;charset=UTF-8");
        });
        return filter;
    }

    @Bean
    public CookieCsrfTokenRepository getCookieCsrfTokenRepository() {
        return new CookieCsrfTokenRepository();
    }
}


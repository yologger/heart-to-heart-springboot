package com.yologger.heart_to_heart_springboot.service;

import com.yologger.heart_to_heart_springboot.controller.api.v1.auth.dto.JoinRequestDTO;
import com.yologger.heart_to_heart_springboot.controller.api.v1.auth.dto.LogInRequestDTO;
import com.yologger.heart_to_heart_springboot.controller.api.v1.auth.dto.TokenRequestDTO;
import com.yologger.heart_to_heart_springboot.controller.service.AuthService;
import com.yologger.heart_to_heart_springboot.repository.MemberRepository;
import com.yologger.heart_to_heart_springboot.repository.entity.MemberEntity;
import com.yologger.heart_to_heart_springboot.security.exception.InvalidPasswordException;
import com.yologger.heart_to_heart_springboot.security.exception.MemberDoesNotExistException;
import com.yologger.heart_to_heart_springboot.util.JwtManager;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import netscape.javascript.JSObject;
import org.json.simple.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtManager jwtManager;

    @Transactional
    @Override
    public ResponseEntity<JSObject> join(JoinRequestDTO request) {

        String email = request.getEmail();
        String fullName = request.getFullName();
        String nickname = request.getNickname();
        String password = request.getPassword();

        // Check if fields are valid.
        if (email == null) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -1);
            responseBody.put("error", "'email' field must not be empty.");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (fullName == null) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -2);
            responseBody.put("error", "'full_name' field must not be empty.");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (nickname == null) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -3);
            responseBody.put("error", "'nickname' field must not be empty.");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (password == null) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -4);
            responseBody.put("error", "'password' field must not be empty.");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // Check if member already exists.
        Optional<MemberEntity> result = memberRepository.findByEmail(email);

        // In case member already exists.
        if (result.isPresent()) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -5);
            responseBody.put("error", "Member already exists.");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        String encryptedPassword = passwordEncoder.encode(password);

        MemberEntity newMember = MemberEntity.builder()
                .email(email)
                .fullName(fullName)
                .nickname(nickname)
                .password(encryptedPassword)
                .build();

        try {
            memberRepository.save(newMember);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject data = new JSONObject();
            data.put("email", email);
            data.put("full_name", fullName);
            data.put("nickname", nickname);

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.CREATED.value());
            responseBody.put("code", 1);
            responseBody.put("message", "Successfully joined.");
            responseBody.put("data", data);

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -6);
            responseBody.put("error", e.getLocalizedMessage());

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<JSObject> logIn(LogInRequestDTO request) throws MemberDoesNotExistException, InvalidPasswordException {

        String email = request.getEmail();
        String password = request.getPassword();

        // Verify request body.
        if (email == null) {

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -1);
            responseBody.put("error", "'email' field must not be empty.");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (password == null) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -2);
            responseBody.put("error", "'password' field must not be empty.");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // Check if user already exists.
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberDoesNotExistException("Member does not exists"));

        // Check if password is correct
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new InvalidPasswordException("Invalid password");
        }

        // Authenticate
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        // Principal: 본인
        MemberEntity me = (MemberEntity) authentication.getPrincipal();
        Long meId = me.getId();
        String meEmail = me.getEmail();
        String meFullName = me.getFullName();
        String meNickname = me.getNickname();

        // Generate tokens
        String accessToken = jwtManager.generateAccessToken(meId, meEmail, meFullName, meNickname);
        String refreshToken = jwtManager.generateRefreshToken(meId, meEmail, meFullName, meNickname);

        member.setAccessToken(accessToken);
        member.setRefreshToken(refreshToken);

        // Response
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        JSONObject data = new JSONObject();
        data.put("user_id", me.getId());
        data.put("email", me.getEmail());
        data.put("full_name", me.getFullName());
        data.put("nickname", me.getNickname());
        data.put("avatar_url", me.getAvatarUrl());
        data.put("access_token", accessToken);
        data.put("refresh_token", refreshToken);

        JSONObject responseBody = new JSONObject();
        responseBody.put("timestamp", LocalDateTime.now());
        responseBody.put("status", HttpStatus.CREATED.value());
        responseBody.put("code", 1);
        responseBody.put("message", "Successfully logged in.");
        responseBody.put("data", data);

        return new ResponseEntity(responseBody, responseHeaders, HttpStatus.CREATED);
    }

    @Transactional
    @Override
    public ResponseEntity<JSObject> token(TokenRequestDTO request) {

        // Check if 'user_id' field exists.
        Long memberId = request.getId();
        if (memberId == null) {

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -1);
            responseBody.put("error", "'user_id' field must not be empty.");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // Check if 'refresh_token' field exists.
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null) {

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -2);
            responseBody.put("error", "'refresh_token' field must not be empty.");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // Compare with ex-refresh token
        Optional<MemberEntity> result = memberRepository.findById(memberId);

        if (result.isEmpty()) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -3);
            responseBody.put("error", "Invalid refresh token");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (!(refreshToken.equals(result.get().getRefreshToken()))) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.BAD_REQUEST.value());
            responseBody.put("code", -4);
            responseBody.put("error", "Invalid refresh token");

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        try {
            // Verify refresh token
            jwtManager.verifyRefreshToken(refreshToken);

            // Reissue access token, refresh token
            MemberEntity member = result.get();

            String newAccessToken = jwtManager.generateAccessToken(member.getId(), member.getEmail(), member.getFullName(), member.getNickname());
            String newRefreshToken = jwtManager.generateRefreshToken(member.getId(), member.getEmail(), member.getFullName(), member.getNickname());

            member.setAccessToken(newAccessToken);
            member.setRefreshToken(newRefreshToken);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject data = new JSONObject();
            data.put("user_id", member.getId());
            data.put("email", member.getEmail());
            data.put("full_name", member.getFullName());
            data.put("nickname", member.getNickname());
            data.put("access_token", newAccessToken);
            data.put("refresh_token", newRefreshToken);
            data.put("image_url", member.getAvatarUrl());

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.OK.value());
            responseBody.put("code", -5);
            responseBody.put("message", "Successfully reissued.");
            responseBody.put("data", data);

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.OK);

        } catch (UnsupportedEncodingException e) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.UNAUTHORIZED.value());
            responseBody.put("code", -5);
            responseBody.put("error", e.getLocalizedMessage());

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.UNAUTHORIZED.value());
            responseBody.put("code", -6);
            responseBody.put("error", e.getLocalizedMessage());

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.UNAUTHORIZED);
        } catch (MalformedJwtException e) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.UNAUTHORIZED.value());
            responseBody.put("code", -7);
            responseBody.put("error", e.getLocalizedMessage());

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.UNAUTHORIZED);
        } catch (SignatureException e) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.UNAUTHORIZED.value());
            responseBody.put("code", -8);
            responseBody.put("error", e.getLocalizedMessage());

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.UNAUTHORIZED);
        } catch (ExpiredJwtException e) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.UNAUTHORIZED.value());
            responseBody.put("code", -9);
            responseBody.put("error", e.getLocalizedMessage());

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.UNAUTHORIZED);
        } catch (IllegalArgumentException e) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.UNAUTHORIZED.value());
            responseBody.put("code", -10);
            responseBody.put("error", e.getLocalizedMessage());

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<JSObject> logout(String authHeader) {

        String accessToken = authHeader.substring(7);

        try {
            Long memberId = jwtManager.verifyAccessTokenAndGetMemberId(accessToken);

            Optional<MemberEntity> result = memberRepository.findById(memberId);

            if (result.isEmpty()) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

                JSONObject responseBody = new JSONObject();
                responseBody.put("timestamp", LocalDateTime.now());
                responseBody.put("status", HttpStatus.BAD_REQUEST.value());
                responseBody.put("error", "User does not exists.");
                responseBody.put("code", -13);

                return new ResponseEntity(responseBody, responseHeaders, HttpStatus.BAD_REQUEST);
            }

            MemberEntity member = result.get();
            member.clearAccessToken();
            member.clearRefreshToken();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.OK.value());
            responseBody.put("message", "Successfully logged out.");
            responseBody.put("code", 1);

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.OK);

        } catch (UnsupportedEncodingException e) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

            JSONObject responseBody = new JSONObject();
            responseBody.put("timestamp", LocalDateTime.now());
            responseBody.put("status", HttpStatus.UNAUTHORIZED.value());
            responseBody.put("code", -14);
            responseBody.put("error", e.getLocalizedMessage());

            return new ResponseEntity(responseBody, responseHeaders, HttpStatus.UNAUTHORIZED);
        }
    }
}

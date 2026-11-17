package com.andre.apiheroi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Hidden
@Controller
public class SwaggerPageController {

    @GetMapping(value = "/swagger-ui/index.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public Resource paginaSwagger() {
        return new ClassPathResource("static/swagger-ui/index.html");
    }
}

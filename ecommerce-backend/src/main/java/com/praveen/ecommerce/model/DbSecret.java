package com.praveen.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DbSecret {
    private String username;
    private String password;
    private String host;
    private String port;
    private String dbname;
}
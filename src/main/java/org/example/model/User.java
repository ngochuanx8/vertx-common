package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
    
    private String id;
    private String name;
    private String email;
    
    public User() {}
    
    @JsonCreator
    public User(@JsonProperty("id") String id, 
                @JsonProperty("name") String name, 
                @JsonProperty("email") String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    @Override
    public String toString() {
        return "User{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", email='" + email + '\'' +
               '}';
    }
}
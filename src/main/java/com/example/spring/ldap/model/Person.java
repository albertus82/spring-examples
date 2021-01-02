package com.example.spring.ldap.model;

import javax.naming.Name;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import lombok.Data;

@Data
@Entry(base = "dc=example,dc=com", objectClasses = "person")
public class Person {

	@Id
	private Name dn;

	@Attribute(name = "sn")
	private String sn;

	@Attribute(name = "givenName")
	private String givenName;

}

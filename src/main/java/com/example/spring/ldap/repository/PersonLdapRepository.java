package com.example.spring.ldap.repository;

import java.util.List;

import org.springframework.data.ldap.repository.LdapRepository;

import com.example.spring.ldap.model.Person;

public interface PersonLdapRepository extends LdapRepository<Person> {

	List<Person> findBySnLikeIgnoreCase(String value);

}

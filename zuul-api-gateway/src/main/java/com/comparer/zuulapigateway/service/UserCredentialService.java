package com.comparer.zuulapigateway.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.comparer.zuulapigateway.repository.UserCredentialsRepository;
import com.comparer.zuulapigateway.resource.LoginCredentials;

@Service
public class UserCredentialService implements UserDetailsService {

	@Autowired
	UserCredentialsRepository repository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Optional<LoginCredentials> user = repository.findByUsername(username);
		if (user.isPresent()) {
			return new org.springframework.security.core.userdetails.User(user.get().getUsername(),
					user.get().getPassword(), new ArrayList<>());
		} else {
			throw new UsernameNotFoundException("User not found with username: " + username);
		}
	}

}

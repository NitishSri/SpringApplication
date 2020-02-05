package com.example.RegisterationService.controller;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.RegisterationService.model.RegisterationUserInfo;
import com.example.RegisterationService.repository.RegistrationRepository;
import com.example.RegisterationService.resourceobject.SuccessReport;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@RestController
public class RegistrationController {

	private RegistrationRepository registrationRepository;

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	RestTemplate restTemplate;

	@Value("${login.service.name}")
	private String loginServiceName;

	@Value("${api.gateway.url}")
	private String apiGatewayURL;

	@Autowired
	Environment environment;

	public RegistrationController(RegistrationRepository registrationRepository) {
		this.registrationRepository = registrationRepository;
	}

	@GetMapping("/register")
	@HystrixCommand
	public String registerUser(String username, String firstname, String lastname, String password) {
		logger.info("register user api called");
		if (!validateInput(username, password)) {
			return "Fields cannot be left blank !!";
		}
		final String checkUserUri = "http://" + loginServiceName + "/checkUserExist";
		// final String checkUserUri = apiGatewayURL +"/"+ loginServiceName +
		// "/checkUserExist";

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(checkUserUri).queryParam("username",
				username);
		String apiUrl = builder.toUriString();

		SuccessReport responseEntity = restTemplate.getForObject(apiUrl, SuccessReport.class);
		if (null != responseEntity && "USEREXIST".equals(responseEntity.getStatusCode())) {
			return responseEntity.getStatusMessage();
		}

		// Add Login User Cred
		final String addUserUri = "http://" + loginServiceName + "/addUser";
		// final String addUserUri = apiGatewayURL +"/"+ loginServiceName + "/addUser";
		UriComponentsBuilder addUserbuilder = UriComponentsBuilder.fromUriString(addUserUri)
				.queryParam("username", username).queryParam("password", password);
		String addUserUrl = addUserbuilder.toUriString();
		SuccessReport addUserEntity = restTemplate.getForObject(addUserUrl, SuccessReport.class);
		if (null != addUserEntity && "USERNOTADDED".equals(addUserEntity.getStatusCode())) {
			return addUserEntity.getStatusMessage();
		}

		RegisterationUserInfo userInfo = new RegisterationUserInfo();
		userInfo.setFirstname(firstname);
		userInfo.setLastname(lastname);
		userInfo.setUsername(username);
		RegisterationUserInfo registeredUser = registrationRepository.save(userInfo);

		if (null != registeredUser) {
			return userInfo.getFirstname() + " " + userInfo.getLastname() + " has been registered successfully !!";
		} else {
			return "oops something went wrong with registration service";
		}

	}

	@GetMapping("/status")
	@HystrixCommand(fallbackMethod = "fallBackLoginService")
	public String checkLoginServiceStatus() {
		logger.info("status api called");
		logger.info("server port => {} ", environment.getProperty("local.server.port"));

		final String statusUri = "http://" + loginServiceName + "/status";
		// final String statusUri = apiGatewayURL +"/"+ loginServiceName + "/status";
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(statusUri);
		String apiUrl = builder.toUriString();
		String responseEntity = restTemplate.getForObject(apiUrl, String.class);
		if (responseEntity != null) {
			return "Login Service is running !!";
		}
		return "Nothing";
	}

	public boolean validateInput(String username, String password) {
		if (Strings.isBlank(username) || Strings.isBlank(password)) {
			return false;
		}
		return true;
	}

	public String fallBackLoginService() {
		logger.info("fallback method called");
		return "Login Service is Under Maintenance";
	}

}
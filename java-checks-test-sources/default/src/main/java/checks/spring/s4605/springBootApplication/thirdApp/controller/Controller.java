package checks.spring.s4605.springBootApplication.thirdApp.controller;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller { } // Noncompliant {{'Controller' is not reachable by @ComponentScan or @SpringBootApplication. Either move it to a package configured in @ComponentScan or update your @ComponentScan configuration.}}
//           ^^^^^^^^^^

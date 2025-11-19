package com.test_cdi.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TestApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void test_true() {
		assertTrue(() -> true);
	}
}

package com.feiniaojin.gracefulresponse.boot4;

import org.springframework.boot.test.context.SpringBootTest;

/** Runs the complete release contract with Boot's lazy bean initialization enabled. */
@SpringBootTest(classes = ReleasedExceptionTest.Application.class, properties = {
        "spring.main.lazy-initialization=true",
        "graceful-response.exclude-exception-types[0]=java.lang.IllegalArgumentException",
        "spring.jackson.property-naming-strategy=SNAKE_CASE"
})
class LazyReleasedExceptionTest extends ReleasedExceptionTest { }

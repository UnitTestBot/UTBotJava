package org.utbot.examples.spring.app;

import org.springframework.stereotype.Service;

@Service
public class MyServiceImpl implements MyService {
    @Override
    public String getName() {
        return "impl";
    }
}

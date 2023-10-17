package org.utbot.examples.spring.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyServiceUser {
    private final MyService myService;

    @Autowired
    public MyServiceUser(MyService myService) {
        this.myService = myService;
    }

    public String useMyService() {
        return myService.getName();
    }
}
